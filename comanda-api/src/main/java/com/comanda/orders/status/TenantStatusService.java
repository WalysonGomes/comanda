package com.comanda.orders.status;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.orders.domain.BusinessHoursRepository;
import com.comanda.platform.tenancy.TenantContext;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for {@code tenants.is_open} (design.md Decision 9): the header toggle is
 * the only write surface. Also serves today's {@code business_hours} row so the panel can derive
 * the "dentro do horário e fechado" banner client-side, in {@code America/Fortaleza}.
 */
@Service
public class TenantStatusService {

    private static final ZoneId TENANT_ZONE = ZoneId.of("America/Fortaleza");

    private final TenantRepository tenantRepository;
    private final BusinessHoursRepository businessHoursRepository;

    public TenantStatusService(TenantRepository tenantRepository, BusinessHoursRepository businessHoursRepository) {
        this.tenantRepository = tenantRepository;
        this.businessHoursRepository = businessHoursRepository;
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
        short today = (short) (LocalDate.now(TENANT_ZONE).getDayOfWeek().getValue() % 7);
        return businessHoursRepository.findByDayOfWeek(today)
                .map(hours -> new TenantStatusResponse(
                        tenant.getName(), tenant.isOpen(), hours.isClosed(), hours.getOpensAt(), hours.getClosesAt()))
                .orElseGet(() -> new TenantStatusResponse(tenant.getName(), tenant.isOpen(), true, null, null));
    }
}
