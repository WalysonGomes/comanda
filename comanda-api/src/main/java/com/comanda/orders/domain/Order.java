package com.comanda.orders.domain;

import com.comanda.platform.tenancy.TenantScopedEntity;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Root of a placed order. {@code idempotencyKey} carries the {@code UNIQUE (tenant_id,
 * idempotency_key)} constraint from {@code foundations}' V1 migration — the sole source of truth
 * for "don't duplicate" (design.md Decision 1); status changes always go through {@link
 * OrderStatusService}, never a direct setter from a controller.
 */
@Entity
@Table(name = "orders")
public class Order extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    private DeliveryType deliveryType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "address_snapshot", columnDefinition = "text")
    private String addressSnapshot;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected Order() {
    }

    public Order(
            Long tenantId, Customer customer, DeliveryType deliveryType, BigDecimal subtotal,
            BigDecimal deliveryFee, BigDecimal total, String addressSnapshot, String notes, String idempotencyKey) {
        super(tenantId);
        this.customer = customer;
        this.status = OrderStatus.RECEBIDO;
        this.deliveryType = deliveryType;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = total;
        this.addressSnapshot = addressSnapshot;
        this.notes = notes;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    /** Set once, right after the items are resolved and priced server-side (Decision 3). */
    public void setTotals(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total) {
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = total;
    }

    public String getAddressSnapshot() {
        return addressSnapshot;
    }

    public String getNotes() {
        return notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** "ID curto" for the panel/customer-facing message (PRD 3.1/5.3): stable, no extra column. */
    public String shortCode() {
        return "#" + Long.toHexString(id).toUpperCase();
    }
}
