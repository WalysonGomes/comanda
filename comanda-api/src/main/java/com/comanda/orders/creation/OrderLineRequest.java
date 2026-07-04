package com.comanda.orders.creation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @Min(value = 1, message = "Quantidade deve ser ao menos 1.") int quantity,
        List<Long> additionalItemIds) {

    public List<Long> additionalItemIds() {
        return additionalItemIds == null ? List.of() : additionalItemIds;
    }
}
