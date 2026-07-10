package com.comanda.plans;

/**
 * Translated to HTTP 402 by each consuming package's own {@code @RestControllerAdvice} (design.md
 * Decision 1) — {@code menu}, {@code orders} and {@code onboarding} each register a handler for
 * this type, mirroring how {@code orders} already re-handles {@code menu}'s not-found exceptions.
 */
public class PlanLimitExceededException extends RuntimeException {

    private final String code;

    private PlanLimitExceededException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static PlanLimitExceededException products() {
        return new PlanLimitExceededException(
                "PRODUCT_LIMIT_REACHED",
                "Você atingiu o limite de " + PlanLimits.MAX_PRODUCTS + " produtos do plano Gratuito.");
    }

    public static PlanLimitExceededException categories() {
        return new PlanLimitExceededException(
                "CATEGORY_LIMIT_REACHED",
                "Você atingiu o limite de " + PlanLimits.MAX_CATEGORIES + " categorias do plano Gratuito.");
    }

    public static PlanLimitExceededException orders() {
        return new PlanLimitExceededException(
                "PLAN_LIMIT_REACHED",
                "Este negócio atingiu o limite de " + PlanLimits.MAX_ORDERS_PER_MONTH
                        + " pedidos do mês no plano Gratuito.");
    }

    public String getCode() {
        return code;
    }
}
