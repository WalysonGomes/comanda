package com.comanda.menu.products.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProductReorderRequest(
        @NotNull(message = "Categoria é obrigatória.") Long categoryId,
        @NotEmpty(message = "Lista de ids é obrigatória.") List<@NotNull Long> orderedIds) {
}
