package com.comanda.plans;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.platform.tenancy.TenantContext;
import com.comanda.storefront.BusinessClock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

/**
 * Order-history retention as a read filter, never a delete (design.md Decision 4 / PRD Seção 6):
 * Gratuito sees the last {@value PlanLimits#RETENTION_DAYS_GRATUITO} days, Essencial the last
 * {@value PlanLimits#RETENTION_DAYS_ESSENCIAL}. {@code order-operation}'s panel applies {@link
 * #cutoff()} to its listing/detail queries; the rows themselves are untouched, so raising the
 * plan immediately re-exposes older orders.
 */
@Service
public class PlanRetentionService {

    private final TenantRepository tenantRepository;

    public PlanRetentionService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public OffsetDateTime cutoff() {
        Tenant tenant = tenantRepository.findById(TenantContext.get()).orElseThrow();
        int days = Plan.ESSENCIAL.name().equals(tenant.getPlan())
                ? PlanLimits.RETENTION_DAYS_ESSENCIAL
                : PlanLimits.RETENTION_DAYS_GRATUITO;
        return OffsetDateTime.now(BusinessClock.ZONE).minusDays(days);
    }
}
