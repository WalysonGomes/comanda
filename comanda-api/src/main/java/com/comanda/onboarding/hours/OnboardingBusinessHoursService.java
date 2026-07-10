package com.comanda.onboarding.hours;

import com.comanda.platform.tenancy.TenantContext;
import com.comanda.storefront.domain.BusinessHours;
import com.comanda.storefront.domain.BusinessHoursRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onboarding step 2 (task 5.3/6.4): upserts the 7 {@code business_hours} rows for the current
 * tenant, reusing {@code public-storefront}'s {@link BusinessHoursRepository} rather than a
 * second read/write model of the same table (same reasoning as {@code order-operation}'s {@code
 * TenantStatusService}, which reuses it for reads).
 */
@Service
public class OnboardingBusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;

    public OnboardingBusinessHoursService(BusinessHoursRepository businessHoursRepository) {
        this.businessHoursRepository = businessHoursRepository;
    }

    public record DayHours(short dayOfWeek, java.time.LocalTime opensAt, java.time.LocalTime closesAt, boolean closed) {
    }

    @Transactional
    public void save(List<DayHours> rows) {
        Long tenantId = TenantContext.get();
        for (DayHours row : rows) {
            BusinessHours existing = businessHoursRepository.findByDayOfWeek(row.dayOfWeek()).orElse(null);
            if (existing != null) {
                existing.update(row.opensAt(), row.closesAt(), row.closed());
            } else {
                businessHoursRepository.save(
                        new BusinessHours(tenantId, row.dayOfWeek(), row.opensAt(), row.closesAt(), row.closed()));
            }
        }
    }
}
