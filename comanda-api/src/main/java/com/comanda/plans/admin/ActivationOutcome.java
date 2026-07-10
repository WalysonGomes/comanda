package com.comanda.plans.admin;

import com.comanda.auth.domain.Tenant;
import com.comanda.plans.domain.Subscription;

/**
 * {@code tenants.plan} is the authoritative value {@code PlanPolicyService} enforces against;
 * {@code subscriptions.plan} is the billing record of what the (possibly now-cancelled)
 * subscription was for. A cancelled subscription still says {@code ESSENCIAL} — that's correct,
 * it's history — so the response the operator sees has to read the tenant's plan, not the
 * subscription's, or a downgrade would misreport itself as having kept the tenant on Essencial.
 */
public record ActivationOutcome(Tenant tenant, Subscription subscription) {
}
