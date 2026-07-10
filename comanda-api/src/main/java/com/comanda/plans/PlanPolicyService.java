package com.comanda.plans;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.menu.domain.CategoryRepository;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;

/**
 * Single source of the "can this tenant create one more X?" rule (design.md Decision 1). Reads
 * only {@code tenants.plan} (+ counts / {@code order_count_month}) — never how the plan was set
 * (PRD Regra 16). Callers (menu-management's product/category creation, order-operation's order
 * creation) decide what to do with a {@code false}; this service never persists anything, so it's
 * safe to call from within another transaction or before one starts.
 */
@Service
public class PlanPolicyService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public PlanPolicyService(
            TenantRepository tenantRepository, ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public boolean canCreateProduct() {
        Tenant tenant = currentTenant();
        return isEssencial(tenant) || productRepository.count() < PlanLimits.MAX_PRODUCTS;
    }

    public boolean canCreateCategory() {
        Tenant tenant = currentTenant();
        return isEssencial(tenant) || categoryRepository.count() < PlanLimits.MAX_CATEGORIES;
    }

    public boolean canCreateOrder() {
        Tenant tenant = currentTenant();
        if (isEssencial(tenant)) {
            return true;
        }
        int effectiveCount = tenant.effectiveOrderCountMonth(PlanPeriod.current());
        return effectiveCount < PlanLimits.MAX_ORDERS_PER_MONTH;
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get()).orElseThrow();
    }

    private boolean isEssencial(Tenant tenant) {
        return Plan.ESSENCIAL.name().equals(tenant.getPlan());
    }
}
