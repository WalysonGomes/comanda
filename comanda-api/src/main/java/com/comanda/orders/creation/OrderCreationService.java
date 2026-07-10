package com.comanda.orders.creation;

import com.comanda.orders.domain.Order;
import com.comanda.orders.domain.OrderRepository;
import com.comanda.plans.PlanLimitExceededException;
import com.comanda.plans.PlanPolicyService;
import com.comanda.platform.tenancy.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Public entry point for order creation (design.md Decision 1). Checks for an existing order by
 * {@code (tenant_id, idempotency_key)} up front — the common retry case never touches the unique
 * constraint at all — and falls back to reading the existing row if a concurrent request won the
 * insert race, so two simultaneous retries with the same key never produce two orders. The plan
 * limit (design.md plans-and-onboarding Decision 2) is checked only on that fresh-creation path —
 * an idempotent retry of an already-created order never re-evaluates or re-counts against it.
 */
@Service
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final OrderCreationTransaction transaction;
    private final PlanPolicyService planPolicyService;

    public OrderCreationService(
            OrderRepository orderRepository, OrderCreationTransaction transaction, PlanPolicyService planPolicyService) {
        this.orderRepository = orderRepository;
        this.transaction = transaction;
        this.planPolicyService = planPolicyService;
    }

    public OrderCreationOutcome create(OrderCreationRequest request) {
        Long tenantId = TenantContext.get();

        Order existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return new OrderCreationOutcome(OrderResponse.from(existing), false);
        }

        if (!planPolicyService.canCreateOrder()) {
            throw PlanLimitExceededException.orders();
        }

        try {
            return new OrderCreationOutcome(OrderResponse.from(transaction.persistNew(tenantId, request)), true);
        } catch (DataIntegrityViolationException raced) {
            return orderRepository.findByIdempotencyKey(request.idempotencyKey())
                    .map(order -> new OrderCreationOutcome(OrderResponse.from(order), false))
                    .orElseThrow(() -> raced);
        }
    }
}
