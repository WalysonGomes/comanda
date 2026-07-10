package com.comanda.plans.admin;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.plans.Plan;
import com.comanda.plans.domain.Subscription;
import com.comanda.plans.domain.SubscriptionRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The MVP's manual billing surface (PRD Seção 10 / design.md Decision 5): elevates or downgrades
 * a tenant by id, restricted to the operator (never reachable from an OWNER's own session — see
 * {@code AdminPlanController} and {@code AdminAuthFilter}). Writes {@code tenants.plan}/{@code
 * plan_expires_at} and {@code subscriptions} together; {@code external_subscription_id} stays
 * null until Stripe integrates in Fase 2. The 30-day period end is informative only in the
 * MVP — nothing expires it automatically; a missed manual renewal is corrected by another manual
 * downgrade (PRD Seção 10 "cobrança manual").
 */
@Service
public class PlanActivationService {

    private static final int ESSENCIAL_PERIOD_DAYS = 30;

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PlanActivationService(TenantRepository tenantRepository, SubscriptionRepository subscriptionRepository) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public ActivationOutcome activateEssencial(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(AdminTenantNotFoundException::new);

        OffsetDateTime periodEnd = OffsetDateTime.now().plusDays(ESSENCIAL_PERIOD_DAYS);
        tenant.setPlan(Plan.ESSENCIAL.name());
        tenant.setPlanExpiresAt(periodEnd);
        tenant = tenantRepository.save(tenant);

        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> new Subscription(tenantId));
        subscription.activate(Plan.ESSENCIAL.name(), periodEnd);
        return new ActivationOutcome(tenant, subscriptionRepository.save(subscription));
    }

    @Transactional
    public ActivationOutcome downgradeToGratuito(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(AdminTenantNotFoundException::new);

        tenant.setPlan(Plan.GRATUITO.name());
        tenant.setPlanExpiresAt(null);
        tenant = tenantRepository.save(tenant);

        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> new Subscription(tenantId));
        subscription.cancel(OffsetDateTime.now());
        return new ActivationOutcome(tenant, subscriptionRepository.save(subscription));
    }
}
