package com.comanda.plans;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.menu.domain.CategoryRepository;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.platform.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Read side for task 7 (Plano e uso) / task 8 (Meu link) — see {@link PlanStatusResponse}. */
@Service
public class PlanStatusService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final String appDomain;

    public PlanStatusService(
            TenantRepository tenantRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            @Value("${app.domain}") String appDomain) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.appDomain = appDomain;
    }

    public PlanStatusResponse get() {
        Tenant tenant = tenantRepository.findById(TenantContext.get()).orElseThrow();
        boolean essencial = Plan.ESSENCIAL.name().equals(tenant.getPlan());

        int orderCount = tenant.effectiveOrderCountMonth(PlanPeriod.current());
        long productCount = productRepository.count();
        long categoryCount = categoryRepository.count();

        Integer orderLimit = essencial ? null : PlanLimits.MAX_ORDERS_PER_MONTH;
        Integer productLimit = essencial ? null : PlanLimits.MAX_PRODUCTS;
        Integer categoryLimit = essencial ? null : PlanLimits.MAX_CATEGORIES;
        boolean showQuotaWarning = !essencial && orderCount >= PlanLimits.ORDER_QUOTA_WARNING_THRESHOLD;

        return new PlanStatusResponse(
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getSubdomain() + "." + appDomain,
                tenant.getPlan(),
                orderCount,
                orderLimit,
                showQuotaWarning,
                productCount,
                productLimit,
                categoryCount,
                categoryLimit);
    }
}
