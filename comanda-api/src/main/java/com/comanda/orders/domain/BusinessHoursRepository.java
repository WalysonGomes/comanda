package com.comanda.orders.domain;

import com.comanda.platform.tenancy.TenantScopedRepository;
import java.util.Optional;

public interface BusinessHoursRepository extends TenantScopedRepository<BusinessHours, Long> {

    Optional<BusinessHours> findByDayOfWeek(short dayOfWeek);
}
