package com.comanda.storefront.cart.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ValidateCartLineRequest(
        @NotBlank(message = "Linha do carrinho sem identificador.") String lineId,
        @NotNull(message = "Produto obrigatório.") Long productId,
        @Min(value = 1, message = "Quantidade deve ser ao menos 1.") int quantity,
        List<Long> additionalItemIds) {

    public ValidateCartLineRequest {
        additionalItemIds = additionalItemIds == null ? List.of() : additionalItemIds;
    }
}
