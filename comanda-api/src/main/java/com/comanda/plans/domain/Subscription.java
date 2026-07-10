package com.comanda.plans.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Plain entity, not a {@link com.comanda.platform.tenancy.TenantScopedEntity}: the operator
 * surface that writes this table (design.md Decision 5) runs with no {@code TenantContext} — it
 * targets an arbitrary tenant by id, which the Hibernate tenant filter would otherwise block.
 * {@code plan} mirrors {@code Tenant.plan}'s convention of a plain string, not the {@code Plan}
 * enum directly, for the same upstream/downstream reason (see {@code Tenant}'s javadoc).
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "plan", nullable = false, length = 20)
    private String plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /** Nullable in the MVP (manual billing); Stripe fills it in Fase 2 without a disruptive
     * migration (PRD Seção 7.5/10). */
    @Column(name = "external_subscription_id", length = 100)
    private String externalSubscriptionId;

    protected Subscription() {
    }

    public Subscription(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getExternalSubscriptionId() {
        return externalSubscriptionId;
    }

    public void activate(String plan, OffsetDateTime currentPeriodEnd) {
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelledAt = null;
    }

    public void cancel(OffsetDateTime cancelledAt) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }
}
