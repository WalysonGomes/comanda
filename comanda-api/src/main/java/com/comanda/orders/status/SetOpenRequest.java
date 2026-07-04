package com.comanda.orders.status;

import jakarta.validation.constraints.NotNull;

public record SetOpenRequest(@NotNull(message = "open é obrigatório.") Boolean open) {
}
