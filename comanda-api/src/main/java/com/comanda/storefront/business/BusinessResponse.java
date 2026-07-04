package com.comanda.storefront.business;

import java.math.BigDecimal;

public record BusinessResponse(
        String name,
        String logoUrl,
        String whatsappNumber,
        BigDecimal deliveryFee,
        BigDecimal minOrderValue,
        boolean open,
        String hoursLabel,
        String reopensLabel) {
}
