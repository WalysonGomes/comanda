package com.comanda.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The tenancy root created by signup. Only the columns owner-auth writes/reads are mapped; the
 * rest of the {@code tenants} row (plan, delivery fee, hours, ...) belongs to later changes and
 * keeps its DB defaults.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 63)
    private String subdomain;

    @Column(nullable = false)
    private String name;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    protected Tenant() {
    }

    public Tenant(String subdomain, String name, String whatsappNumber) {
        this.subdomain = subdomain;
        this.name = name;
        this.whatsappNumber = whatsappNumber;
    }

    public Long getId() {
        return id;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getName() {
        return name;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }
}
