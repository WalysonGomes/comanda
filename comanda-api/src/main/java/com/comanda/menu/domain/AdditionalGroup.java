package com.comanda.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Not a {@link com.comanda.platform.tenancy.TenantScopedEntity}: {@code additional_groups} has no
 * {@code tenant_id} column (design.md Decision 1 — ownership flows group → product → tenant).
 * Cross-tenant access is guarded by {@link AdditionalGroupRepository#findByIdAndTenantId}, never
 * by the Hibernate filter.
 */
@Entity
@Table(name = "additional_groups")
public class AdditionalGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false)
    private SelectionType selectionType;

    @Column(name = "min_selections", nullable = false)
    private Integer minSelections;

    @Column(name = "max_selections")
    private Integer maxSelections;

    protected AdditionalGroup() {
    }

    public AdditionalGroup(
            Product product, String name, boolean required, SelectionType selectionType,
            Integer minSelections, Integer maxSelections) {
        this.product = product;
        this.name = name;
        this.required = required;
        this.selectionType = selectionType;
        this.minSelections = minSelections;
        this.maxSelections = maxSelections;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(SelectionType selectionType) {
        this.selectionType = selectionType;
    }

    public Integer getMinSelections() {
        return minSelections;
    }

    public void setMinSelections(Integer minSelections) {
        this.minSelections = minSelections;
    }

    public Integer getMaxSelections() {
        return maxSelections;
    }

    public void setMaxSelections(Integer maxSelections) {
        this.maxSelections = maxSelections;
    }
}
