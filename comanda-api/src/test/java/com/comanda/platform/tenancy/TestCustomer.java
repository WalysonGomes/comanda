package com.comanda.platform.tenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test-only mapping onto the real {@code customers} table, used solely to prove the
 * {@link TenantScopedEntity} filter mechanism against an actual MVP table (no production
 * entities exist yet for the {@code menu}/{@code orders} feature slices).
 */
@Entity
@Table(name = "customers")
class TestCustomer extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String phone;

    protected TestCustomer() {
    }

    Long getId() {
        return id;
    }
}
