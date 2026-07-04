package com.comanda.orders.panel;

import com.comanda.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** {@code fromStatus} is the status the panel currently sees — the idempotency key of the advance
 * (design.md Decision 4). */
public record AdvanceOrderRequest(@NotNull(message = "fromStatus é obrigatório.") OrderStatus fromStatus) {
}
