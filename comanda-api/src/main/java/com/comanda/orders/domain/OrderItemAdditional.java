package com.comanda.orders.domain;

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
 * Not tenant-scoped: ownership flows additional → item → order → tenant. Immutable snapshot, same
 * rationale as {@link OrderItem}.
 */
@Entity
@Table(name = "order_item_additionals")
public class OrderItemAdditional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "additional_name_snapshot", nullable = false)
    private String additionalNameSnapshot;

    @Column(name = "additional_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPriceSnapshot;

    protected OrderItemAdditional() {
    }

    public OrderItemAdditional(OrderItem orderItem, String additionalNameSnapshot, BigDecimal additionalPriceSnapshot) {
        this.orderItem = orderItem;
        this.additionalNameSnapshot = additionalNameSnapshot;
        this.additionalPriceSnapshot = additionalPriceSnapshot;
    }

    public Long getId() {
        return id;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public String getAdditionalNameSnapshot() {
        return additionalNameSnapshot;
    }

    public BigDecimal getAdditionalPriceSnapshot() {
        return additionalPriceSnapshot;
    }
}
