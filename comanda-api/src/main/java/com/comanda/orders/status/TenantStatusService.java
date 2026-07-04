package com.comanda.orders.status;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.platform.tenancy.TenantContext;
import com.comanda.storefront.BusinessClock;
import com.comanda.storefront.domain.BusinessHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for {@code tenants.is_open} (design.md Decision 9): the header toggle is
 * the only write surface. Also serves today's {@code business_hours} row so the panel can derive
 * the "dentro do horário e fechado" banner client-side. Reuses {@code public-storefront}'s {@link
 * BusinessHoursRepository}/{@link BusinessClock} rather than a second read model of the same
 * table — the two changes were built in parallel worktrees and each grew its own copy; this is
 * the post-merge consolidation onto the one with a real day-of-week/timezone test.
 */
@Service
public class TenantStatusService {

    private final TenantRepository tenantRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BusinessClock businessClock;

    public TenantStatusService(
            TenantRepository tenantRepository, BusinessHoursRepository businessHoursRepository, BusinessClock businessClock) {
        this.tenantRepository = tenantRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.businessClock = businessClock;
    }

    public TenantStatusResponse get() {
        Tenant tenant = tenantRepository.findById(TenantContext.get()).orElseThrow();
        return toResponse(tenant);
    }

    @Transactional
    public TenantStatusResponse setOpen(boolean open) {
        Tenant tenant = tenantRepository.findById(TenantContext.get()).orElseThrow();
        tenant.setOpen(open);
        return toResponse(tenant);
    }

    private TenantStatusResponse toResponse(Tenant tenant) {
        short today = (short) businessClock.dayOfWeek();
        return businessHoursRepository.findByDayOfWeek(today)
                .map(hours -> new TenantStatusResponse(
                        tenant.getName(), tenant.isOpen(), hours.isClosed(), hours.getOpensAt(), hours.getClosesAt()))
                .orElseGet(() -> new TenantStatusResponse(tenant.getName(), tenant.isOpen(), true, null, null));
    }
}
