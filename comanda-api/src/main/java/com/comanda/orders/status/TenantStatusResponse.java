package com.comanda.orders.status;

import java.time.LocalTime;

public record TenantStatusResponse(
        String businessName,
        boolean open,
        boolean closedToday,
        LocalTime opensAt,
        LocalTime closesAt) {
}
