package com.comanda.plans;

import com.comanda.storefront.BusinessClock;
import java.time.YearMonth;

/**
 * The "competência" (year-month) used for the lazy reset of {@code order_count_month} (design.md
 * Decision 3): always {@code America/Fortaleza}, same zone as every other date computation in the
 * MVP (PRD Seção 13). Format {@code yyyy-MM} matches the {@code order_count_month_period} column.
 */
public final class PlanPeriod {

    private PlanPeriod() {
    }

    public static String current() {
        return YearMonth.now(BusinessClock.ZONE).toString();
    }
}
