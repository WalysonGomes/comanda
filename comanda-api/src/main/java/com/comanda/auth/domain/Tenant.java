package com.comanda.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The tenancy root created by signup. Owner-auth only writes/reads the identity columns; {@code
 * order-operation} adds the fields it needs for order totals and the header toggle ({@link
 * #deliveryFee}, {@link #minOrderValue}, {@link #open}, {@link #orderCountMonth}) — all already
 * present in the {@code tenants} row since {@code foundations}' V1 migration, just unmapped until
 * a feature needed them.
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

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "min_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    @Column(name = "order_count_month", nullable = false)
    private int orderCountMonth;

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

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getOrderCountMonth() {
        return orderCountMonth;
    }

    public void incrementOrderCountMonth() {
        this.orderCountMonth++;
    }
}
