package com.comanda.plans;

/** Constants for the Gratuito plan's limits (PRD Seção 6, task 1.2) — the single source of truth. */
public final class PlanLimits {

    public static final int MAX_PRODUCTS = 30;
    public static final int MAX_CATEGORIES = 5;
    public static final int MAX_ORDERS_PER_MONTH = 30;
    public static final int ORDER_QUOTA_WARNING_THRESHOLD = 25;

    public static final int RETENTION_DAYS_GRATUITO = 7;
    public static final int RETENTION_DAYS_ESSENCIAL = 30;

    private PlanLimits() {
    }
}
