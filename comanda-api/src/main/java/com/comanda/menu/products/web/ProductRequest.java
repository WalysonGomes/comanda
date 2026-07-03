package com.comanda.menu.products.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Nome do produto é obrigatório.") String name,
        String description,
        @NotNull(message = "Preço é obrigatório.")
        @DecimalMin(value = "0", inclusive = true, message = "Preço não pode ser negativo.")
        BigDecimal price,
        Boolean available,
        Integer[] availableDays,
        @NotNull(message = "Categoria é obrigatória.") Long categoryId,
        String imageUrl) {
}
