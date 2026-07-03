package com.comanda.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Not a {@link com.comanda.platform.tenancy.TenantScopedEntity}: no {@code tenant_id} column;
 * ownership is item → group → product → tenant, guarded by
 * {@link AdditionalItemRepository#findByIdAndTenantId}.
 */
@Entity
@Table(name = "additional_items")
public class AdditionalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "additional_group_id", nullable = false)
    private AdditionalGroup additionalGroup;

    @Column(nullable = false)
    private String name;

    @Column(name = "additional_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    protected AdditionalItem() {
    }

    public AdditionalItem(AdditionalGroup additionalGroup, String name, BigDecimal additionalPrice) {
        this.additionalGroup = additionalGroup;
        this.name = name;
        this.additionalPrice = additionalPrice;
    }

    public Long getId() {
        return id;
    }

    public AdditionalGroup getAdditionalGroup() {
        return additionalGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public void setAdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
