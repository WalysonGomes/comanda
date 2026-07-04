package com.comanda.storefront.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Read-only projection of {@code tenants} for the fields the public storefront needs
 * (delivery fee, minimum order, open toggle, WhatsApp number...) that {@code owner-auth}'s
 * {@code Tenant} entity deliberately doesn't map (it only owns the columns it writes). Never
 * saved/updated through this entity — write access to {@code tenants} belongs to the slices that
 * created those columns.
 */
@Entity
@Table(name = "tenants")
public class StorefrontTenant {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 63)
    private String subdomain;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "min_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    protected StorefrontTenant() {
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public boolean isOpen() {
        return open;
    }
}
