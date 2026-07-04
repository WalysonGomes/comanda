package com.comanda.orders.creation;

import com.comanda.orders.domain.Order;
import java.math.BigDecimal;

/** "o pedido criado (id curto e total)" — the exact response shape the contract promises. */
public record OrderResponse(Long id, String shortCode, String status, BigDecimal total) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.shortCode(), order.getStatus().name(), order.getTotal());
    }
}
