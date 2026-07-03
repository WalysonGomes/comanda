package com.comanda.menu.products;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        Long categoryId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available,
        Integer position,
        Integer[] availableDays) {
}
