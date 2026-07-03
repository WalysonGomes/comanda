package com.comanda.menu.additionals;

import java.math.BigDecimal;

public record AdditionalItemResponse(Long id, Long groupId, String name, BigDecimal additionalPrice, boolean available) {
}
