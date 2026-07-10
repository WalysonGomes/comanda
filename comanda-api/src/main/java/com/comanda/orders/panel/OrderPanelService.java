package com.comanda.orders.panel;

import com.comanda.orders.InvalidCancellationReasonException;
import com.comanda.orders.InvalidStatusTransitionException;
import com.comanda.orders.OrderNotFoundException;
import com.comanda.orders.OrderStatusService;
import com.comanda.orders.domain.Order;
import com.comanda.orders.domain.OrderItem;
import com.comanda.orders.domain.OrderItemAdditionalRepository;
import com.comanda.orders.domain.OrderItemRepository;
import com.comanda.orders.domain.OrderRepository;
import com.comanda.orders.domain.OrderStatus;
import com.comanda.plans.PlanRetentionService;
import com.comanda.platform.tenancy.UserContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel-side reads and writes: all filtered by {@code tenant_id} from the JWT (via {@link
 * com.comanda.platform.tenancy.TenantScopedEntity}'s Hibernate filter) — a cross-tenant order id
 * simply doesn't come back from {@link OrderRepository#findById}, so it surfaces as {@link
 * OrderNotFoundException} (404), never a data leak (spec "Isolamento por tenant no painel").
 */
@Service
public class OrderPanelService {

    private static final ZoneId TENANT_ZONE = ZoneId.of("America/Fortaleza");
    private static final List<OrderStatus> FILTERS = List.of(
            OrderStatus.RECEBIDO, OrderStatus.ACEITO, OrderStatus.EM_PREPARO,
            OrderStatus.PRONTO, OrderStatus.ENTREGUE, OrderStatus.CANCELADO);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemAdditionalRepository orderItemAdditionalRepository;
    private final OrderStatusService orderStatusService;
    private final PlanRetentionService planRetentionService;

    public OrderPanelService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderItemAdditionalRepository orderItemAdditionalRepository,
            OrderStatusService orderStatusService,
            PlanRetentionService planRetentionService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemAdditionalRepository = orderItemAdditionalRepository;
        this.orderStatusService = orderStatusService;
        this.planRetentionService = planRetentionService;
    }

    public OrdersBoardResponse board(OrderStatus statusFilter) {
        OffsetDateTime retentionCutoff = planRetentionService.cutoff();
        List<Order> filtered = statusFilter == null
                ? orderRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(retentionCutoff)
                : orderRepository.findAllByStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(statusFilter, retentionCutoff);

        List<StatusCountResponse> filters = FILTERS.stream()
                .map(status -> new StatusCountResponse(status.name(), orderRepository.countByStatus(status)))
                .toList();

        OffsetDateTime startOfToday = LocalDate.now(TENANT_ZONE).atStartOfDay(TENANT_ZONE).toOffsetDateTime();
        long todayCount = orderRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        BigDecimal todayRevenue = orderRepository.revenueSince(startOfToday, OrderStatus.CANCELADO);
        long openCount = orderRepository.countByStatusNotIn(List.of(OrderStatus.ENTREGUE, OrderStatus.CANCELADO));

        List<OrderSummaryResponse> orders = filtered.stream().map(this::toSummary).toList();
        return new OrdersBoardResponse(new DaySummaryResponse(todayCount, todayRevenue, openCount), filters, orders);
    }

    public OrderDetailResponse detail(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (order.getCreatedAt().isBefore(planRetentionService.cutoff())) {
            throw new OrderNotFoundException();
        }
        return toDetail(order);
    }

    @Transactional
    public OrderDetailResponse advance(Long orderId, OrderStatus expectedFrom) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        OrderStatus current = order.getStatus();
        if (current.isTerminal()) {
            throw new InvalidStatusTransitionException();
        }

        int currentIndex = OrderStatus.sequenceIndex(current);
        int fromIndex = OrderStatus.sequenceIndex(expectedFrom);
        if (fromIndex < 0 || fromIndex > currentIndex) {
            throw new InvalidStatusTransitionException();
        }
        if (fromIndex < currentIndex) {
            // Already advanced past the origin the panel expected: duplicate tap/retry, no-op.
            return toDetail(order);
        }

        OrderStatus next = current.next().orElseThrow(InvalidStatusTransitionException::new);
        orderStatusService.transition(order, current, next, UserContext.get());
        return toDetail(order);
    }

    @Transactional
    public OrderDetailResponse cancel(Long orderId, String reason) {
        if (reason == null || reason.trim().length() < 10) {
            throw new InvalidCancellationReasonException();
        }
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (order.getStatus().isTerminal()) {
            throw new InvalidStatusTransitionException();
        }

        OrderStatus previous = order.getStatus();
        order.setCancellationReason(reason.trim());
        orderStatusService.transition(order, previous, OrderStatus.CANCELADO, UserContext.get());
        return toDetail(order);
    }

    private OrderSummaryResponse toSummary(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderById(order.getId());
        String summary = summarize(items);
        return new OrderSummaryResponse(
                order.getId(), order.shortCode(), order.getStatus().name(), order.getCustomer().getName(),
                summary, order.getDeliveryType(), order.getTotal(), order.getCreatedAt(),
                order.getStatus() == OrderStatus.RECEBIDO);
    }

    private String summarize(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        OrderItem first = items.get(0);
        String base = first.getQuantity() + "x " + first.getProductNameSnapshot();
        int rest = items.size() - 1;
        if (rest <= 0) {
            return base;
        }
        return base + " · +" + rest + (rest > 1 ? " itens" : " item");
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderById(order.getId());
        List<OrderDetailResponse.ItemView> itemViews = items.stream().map(item -> {
            List<OrderDetailResponse.AdditionalView> additionals =
                    orderItemAdditionalRepository.findAllByOrderItemIdOrderById(item.getId()).stream()
                            .map(a -> new OrderDetailResponse.AdditionalView(a.getAdditionalNameSnapshot(), a.getAdditionalPriceSnapshot()))
                            .toList();
            return new OrderDetailResponse.ItemView(
                    item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(), item.getSubtotal(), additionals);
        }).toList();

        return new OrderDetailResponse(
                order.getId(), order.shortCode(), order.getStatus().name(),
                order.getCustomer().getName(), order.getCustomer().getPhone(),
                order.getDeliveryType(), order.getAddressSnapshot(), order.getNotes(), order.getCancellationReason(),
                order.getSubtotal(), order.getDeliveryFee(), order.getTotal(), order.getCreatedAt(), itemViews);
    }
}
