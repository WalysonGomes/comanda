package com.comanda.menu.categories.web;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "Nome da categoria é obrigatório.") String name,
        Boolean active) {
}
