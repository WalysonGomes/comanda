package com.comanda.menu.additionals.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdditionalItemRequest(
        @NotBlank(message = "Nome do item é obrigatório.") String name,
        @NotNull(message = "Preço adicional é obrigatório.")
        @DecimalMin(value = "0", inclusive = true, message = "Preço adicional não pode ser negativo.")
        BigDecimal additionalPrice,
        Boolean available) {
}
