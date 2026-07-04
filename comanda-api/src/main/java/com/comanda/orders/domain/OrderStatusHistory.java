package com.comanda.orders.domain;

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
import java.time.OffsetDateTime;

/**
 * Append-only by convention (PRD 4.4): only ever inserted, from {@link OrderStatusService}, never
 * updated or deleted. {@code changedByUserId} null means the transition originated from {@code
 * SYSTEM} (the initial record on order creation).
 */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected OrderStatusHistory() {
    }

    public OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, Long changedByUserId, String notes) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
