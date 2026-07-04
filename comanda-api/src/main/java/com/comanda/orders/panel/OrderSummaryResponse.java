package com.comanda.orders.panel;

import com.comanda.orders.domain.DeliveryType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryResponse(
        Long id,
        String shortCode,
        String status,
        String customerName,
        String itemsSummary,
        DeliveryType deliveryType,
        BigDecimal total,
        OffsetDateTime createdAt,
        boolean isNew) {
}
