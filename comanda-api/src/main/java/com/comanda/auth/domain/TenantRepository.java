package com.comanda.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsBySubdomain(String subdomain);
}
