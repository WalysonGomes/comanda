package com.comanda.orders.creation;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.menu.AdditionalItemNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.menu.domain.AdditionalItem;
import com.comanda.menu.domain.AdditionalItemRepository;
import com.comanda.menu.domain.Product;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.orders.ItemUnavailableException;
import com.comanda.orders.OrderStatusService;
import com.comanda.orders.domain.Customer;
import com.comanda.orders.domain.CustomerRepository;
import com.comanda.orders.domain.DeliveryType;
import com.comanda.orders.domain.Order;
import com.comanda.orders.domain.OrderItem;
import com.comanda.orders.domain.OrderItemAdditional;
import com.comanda.orders.domain.OrderItemAdditionalRepository;
import com.comanda.orders.domain.OrderItemRepository;
import com.comanda.orders.domain.OrderRepository;
import com.comanda.orders.domain.OrderStatus;
import com.comanda.storefront.BusinessClock;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The write path of order creation, isolated in its own {@code REQUIRES_NEW} transaction
 * (design.md Decision 1) so a unique-constraint violation on {@code idempotency_key} rolls back
 * only this attempt — never the caller's transaction — letting {@link OrderCreationService} catch
 * the violation and read back the already-created order in a clean transaction of its own.
 */
@Component
class OrderCreationTransaction {

    private final ProductRepository productRepository;
    private final AdditionalItemRepository additionalItemRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemAdditionalRepository orderItemAdditionalRepository;
    private final TenantRepository tenantRepository;
    private final OrderStatusService orderStatusService;
    private final BusinessClock businessClock;

    OrderCreationTransaction(
            ProductRepository productRepository,
            AdditionalItemRepository additionalItemRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderItemAdditionalRepository orderItemAdditionalRepository,
            TenantRepository tenantRepository,
            OrderStatusService orderStatusService,
            BusinessClock businessClock) {
        this.productRepository = productRepository;
        this.additionalItemRepository = additionalItemRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemAdditionalRepository = orderItemAdditionalRepository;
        this.tenantRepository = tenantRepository;
        this.orderStatusService = orderStatusService;
        this.businessClock = businessClock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Order persistNew(Long tenantId, OrderCreationRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        Customer customer = customerRepository.findByPhone(request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(tenantId, request.customerName().trim(), request.customerPhone().trim())));
        if (!customer.getName().equals(request.customerName().trim())) {
            customer.setName(request.customerName().trim());
        }

        boolean isDelivery = request.deliveryType() == DeliveryType.ENTREGA;
        if (isDelivery && (request.address() == null || request.address().isBlank())) {
            throw new IllegalArgumentException("Endereço é obrigatório para entrega.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal deliveryFee = isDelivery ? tenant.getDeliveryFee() : BigDecimal.ZERO;

        Order order = new Order(
                tenantId, customer, request.deliveryType(), BigDecimal.ZERO, deliveryFee, BigDecimal.ZERO,
                isDelivery ? request.address().trim() : null, request.notes(), request.idempotencyKey());
        order = orderRepository.save(order);

        for (OrderLineRequest line : request.lines()) {
            Product product = productRepository.findById(line.productId()).orElseThrow(ProductNotFoundException::new);
            if (!isAvailableToday(product)) {
                throw new ItemUnavailableException(product.getName());
            }

            BigDecimal unitPrice = product.getPrice();
            List<AdditionalItem> additionals = line.additionalItemIds().stream()
                    .map(id -> additionalItemRepository.findByIdAndTenantId(id, tenantId).orElseThrow(AdditionalItemNotFoundException::new))
                    .toList();
            for (AdditionalItem additional : additionals) {
                if (!additional.getAdditionalGroup().getProduct().getId().equals(product.getId())) {
                    throw new AdditionalItemNotFoundException();
                }
                if (!additional.isAvailable()) {
                    throw new ItemUnavailableException(additional.getName());
                }
            }

            BigDecimal additionalsTotal = additionals.stream()
                    .map(AdditionalItem::getAdditionalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal lineSubtotal = unitPrice.add(additionalsTotal).multiply(BigDecimal.valueOf(line.quantity()));
            subtotal = subtotal.add(lineSubtotal);

            OrderItem orderItem = orderItemRepository.save(
                    new OrderItem(order, product.getName(), unitPrice, line.quantity(), lineSubtotal));
            for (AdditionalItem additional : additionals) {
                orderItemAdditionalRepository.save(
                        new OrderItemAdditional(orderItem, additional.getName(), additional.getAdditionalPrice()));
            }
        }

        order.setTotals(subtotal, deliveryFee, subtotal.add(deliveryFee));
        orderStatusService.transition(order, null, OrderStatus.RECEBIDO, null);

        tenant.incrementOrderCountMonth();
        tenantRepository.save(tenant);

        return orderRepository.saveAndFlush(order);
    }

    private boolean isAvailableToday(Product product) {
        if (!product.isAvailable()) {
            return false;
        }
        Integer[] availableDays = product.getAvailableDays();
        if (availableDays == null || availableDays.length == 0) {
            return true;
        }
        return Arrays.asList(availableDays).contains(businessClock.dayOfWeek());
    }
}
