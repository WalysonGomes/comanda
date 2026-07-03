package com.comanda.menu.additionals.web;

import com.comanda.menu.domain.SelectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdditionalGroupRequest(
        @NotBlank(message = "Nome do grupo é obrigatório.") String name,
        boolean required,
        @NotNull(message = "Tipo de seleção é obrigatório.") SelectionType selectionType,
        @NotNull(message = "Mínimo de seleções é obrigatório.") @Min(value = 0, message = "Mínimo não pode ser negativo.") Integer minSelections,
        Integer maxSelections) {
}
