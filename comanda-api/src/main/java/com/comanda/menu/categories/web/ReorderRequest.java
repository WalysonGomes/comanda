package com.comanda.menu.categories.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderRequest(@NotEmpty(message = "Lista de ids é obrigatória.") List<@NotNull Long> orderedIds) {
}
