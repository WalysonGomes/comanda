package com.comanda.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * The tenancy root created by signup. Owner-auth only writes/reads the identity columns; {@code
 * order-operation} adds the fields it needs for order totals and the header toggle ({@link
 * #deliveryFee}, {@link #minOrderValue}, {@link #open}, {@link #orderCountMonth}); {@code
 * plans-and-onboarding} adds {@link #plan}/{@link #planExpiresAt} — all already present in the
 * {@code tenants} row since {@code foundations}' V1 migration, just unmapped until a feature
 * needed them.
 *
 * <p>{@link #plan} is a plain string (not the {@code plans} package's {@code Plan} enum): {@code
 * auth} is the most upstream slice — every later slice depends on it, never the reverse (matches
 * {@code storefront}/{@code orders} each keeping their own small {@code DeliveryType} rather than
 * sharing one). The {@code plans} package parses this value into its own enum.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    /** Duplicates {@code BusinessClock.ZONE} (PRD Seção 13 accepts this hardcode in multiple
     * places until Fase 4) — {@code auth} can't depend on {@code storefront} for one constant. */
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "min_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    @Column(name = "plan", nullable = false, length = 20)
    private String plan = "GRATUITO";

    @Column(name = "plan_expires_at")
    private OffsetDateTime planExpiresAt;

    @Column(name = "order_count_month", nullable = false)
    private int orderCountMonth;

    @Column(name = "order_count_month_period", nullable = false, length = 7)
    private String orderCountMonthPeriod = YearMonth.now(ZONE).toString();

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

    public String getLogoUrl() { return logoUrl; }

    public void updateBusinessSettings(String name, String whatsappNumber, BigDecimal deliveryFee,
            BigDecimal minOrderValue, String subdomain) {
        this.name = name;
        this.whatsappNumber = whatsappNumber;
        this.deliveryFee = deliveryFee;
        this.minOrderValue = minOrderValue;
        this.subdomain = subdomain;
    }

    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

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

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public OffsetDateTime getPlanExpiresAt() {
        return planExpiresAt;
    }

    public void setPlanExpiresAt(OffsetDateTime planExpiresAt) {
        this.planExpiresAt = planExpiresAt;
    }

    public int getOrderCountMonth() {
        return orderCountMonth;
    }

    /** Zero when the stored competência (year-month) isn't the one given — never persists,
     * just the number the caller should reason with (design.md Decision 2/3). */
    public int effectiveOrderCountMonth(String currentPeriod) {
        return currentPeriod.equals(orderCountMonthPeriod) ? orderCountMonth : 0;
    }

    /** Resets to 1 instead of incrementing stale state when the competência rolled over since the
     * last write — the lazy reset (design.md Decision 3), applied at the one place that writes. */
    public void incrementOrderCountMonth(String currentPeriod) {
        if (!currentPeriod.equals(orderCountMonthPeriod)) {
            orderCountMonth = 0;
            orderCountMonthPeriod = currentPeriod;
        }
        orderCountMonth++;
    }
}
