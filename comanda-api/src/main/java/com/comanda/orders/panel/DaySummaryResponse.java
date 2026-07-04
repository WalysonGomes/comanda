package com.comanda.orders.panel;

import java.math.BigDecimal;

public record DaySummaryResponse(long count, BigDecimal revenue, long openCount) {
}
