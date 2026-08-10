package com.comanda.settings;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record BusinessSettingsResponse(String businessName, String logoUrl, String whatsappNumber,
        BigDecimal deliveryFee, BigDecimal minOrderValue, String subdomain, List<DayHours> businessHours) {
    public record DayHours(short dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closed) {}
}
