package com.comanda.plans;

/**
 * Powers both the painel's "Plano e uso" screen (task 7) and "Meu link" (task 8) — both need
 * {@code businessName}/{@code subdomain}/{@code menuUrl}, so one endpoint serves both rather than
 * duplicating a second tenant-info read. Every {@code *Limit} is {@code null} for Essencial
 * (unlimited); the UI renders "ilimitado" instead of a bar when it sees null (task 7.3).
 */
public record PlanStatusResponse(
        String businessName,
        String subdomain,
        String menuUrl,
        String plan,
        int orderCountMonth,
        Integer orderLimit,
        boolean showQuotaWarning,
        long productCount,
        Integer productLimit,
        long categoryCount,
        Integer categoryLimit) {
}
