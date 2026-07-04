package com.comanda.orders.panel;

import com.comanda.orders.domain.DeliveryType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String shortCode,
        String status,
        String customerName,
        String customerPhone,
        DeliveryType deliveryType,
        String address,
        String notes,
        String cancellationReason,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<ItemView> items) {

    public record ItemView(
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            List<AdditionalView> additionals) {
    }

    public record AdditionalView(String name, BigDecimal price) {
    }
}
