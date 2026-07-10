package com.comanda.plans;

/**
 * PRD Seção 6. {@code GRATUITO} carries the enforced limits (task 1.2); {@code ESSENCIAL} is
 * unlimited. The machine that reads this enum never knows how the plan was set — manually today,
 * by Stripe webhook in Fase 2 (PRD Regra 16) — it only reads {@code tenants.plan}.
 */
public enum Plan {
    GRATUITO,
    ESSENCIAL
}
