package com.comanda.orders.creation;

/** Distinguishes a fresh order from an idempotent replay so the controller can pick 201 vs 200. */
public record OrderCreationOutcome(OrderResponse order, boolean created) {
}
