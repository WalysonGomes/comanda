package com.comanda.storefront.business;

import com.comanda.storefront.BusinessClock;
import com.comanda.storefront.TenantNotFoundException;
import com.comanda.storefront.domain.BusinessHours;
import com.comanda.storefront.domain.BusinessHoursRepository;
import com.comanda.storefront.domain.StorefrontTenant;
import com.comanda.storefront.domain.StorefrontTenantRepository;
import com.comanda.platform.tenancy.TenantContext;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * "Aberto agora?" is the AND of the owner's manual toggle ({@code tenants.is_open}) and today's
 * {@code business_hours} row, both read in {@code America/Fortaleza} (design.md Decision 3).
 * Closed never blocks browsing/cart-building — only the storefront's checkout does that,
 * elsewhere.
 */
@Service
public class BusinessService {

    private static final String[] DAY_NAMES =
            {"domingo", "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado"};
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("H'h'mm");

    private final StorefrontTenantRepository tenantRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BusinessClock clock;

    public BusinessService(
            StorefrontTenantRepository tenantRepository,
            BusinessHoursRepository businessHoursRepository,
            BusinessClock clock) {
        this.tenantRepository = tenantRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.clock = clock;
    }

    public BusinessResponse get() {
        StorefrontTenant tenant = tenantRepository.findById(TenantContext.get()).orElseThrow(TenantNotFoundException::new);
        int today = clock.dayOfWeek();
        Optional<BusinessHours> todayHours = businessHoursRepository.findByDayOfWeek((short) today);

        boolean scheduleOpen = todayHours.filter(this::hasWindow).map(h -> withinWindow(h, clock.now())).orElse(false);
        boolean open = tenant.isOpen() && scheduleOpen;

        String hoursLabel = todayHours.filter(this::hasWindow)
                .map(h -> "Hoje " + formatHour(h.getOpensAt()) + " – " + formatHour(h.getClosesAt()))
                .orElse("Fechado hoje");

        String reopensLabel = open ? null : reopensLabel(tenant, scheduleOpen, today);

        return new BusinessResponse(
                tenant.getName(), tenant.getLogoUrl(), tenant.getWhatsappNumber(),
                tenant.getDeliveryFee(), tenant.getMinOrderValue(), open, hoursLabel, reopensLabel);
    }

    private boolean hasWindow(BusinessHours hours) {
        return !hours.isClosed() && hours.getOpensAt() != null && hours.getClosesAt() != null;
    }

    private boolean withinWindow(BusinessHours hours, LocalTime now) {
        return !now.isBefore(hours.getOpensAt()) && !now.isAfter(hours.getClosesAt());
    }

    private String reopensLabel(StorefrontTenant tenant, boolean scheduleOpen, int today) {
        if (!tenant.isOpen() && scheduleOpen) {
            return "Fechado pelo estabelecimento no momento.";
        }
        LocalTime now = clock.now();
        for (int offset = 0; offset < 7; offset++) {
            int day = (today + offset) % 7;
            Optional<BusinessHours> hours = businessHoursRepository.findByDayOfWeek((short) day).filter(this::hasWindow);
            if (hours.isEmpty()) {
                continue;
            }
            LocalTime opensAt = hours.get().getOpensAt();
            if (offset == 0 && !opensAt.isAfter(now)) {
                continue;
            }
            String when = offset == 0 ? "hoje" : (offset == 1 ? "amanhã" : DAY_NAMES[day]);
            return "Abre " + when + " às " + formatHour(opensAt) + ".";
        }
        return "Sem horário de funcionamento configurado.";
    }

    private String formatHour(LocalTime time) {
        return time.getMinute() == 0 ? time.getHour() + "h" : HOUR_FORMAT.format(time);
    }
}
