package com.comanda.orders.panel;

import java.util.List;

public record OrdersBoardResponse(
        DaySummaryResponse summary,
        List<StatusCountResponse> filters,
        List<OrderSummaryResponse> orders) {
}
