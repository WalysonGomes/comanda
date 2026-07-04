package com.comanda.orders.domain;

import com.comanda.platform.tenancy.TenantScopedRepository;
import java.util.Optional;

public interface CustomerRepository extends TenantScopedRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);
}
