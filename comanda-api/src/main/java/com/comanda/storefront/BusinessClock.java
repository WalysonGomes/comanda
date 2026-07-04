package com.comanda.storefront;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * Single source of "now" for availability ({@code available_days}) and open/closed
 * ({@code business_hours}) calculations, hardcoded to {@code America/Fortaleza} (PRD Seção 13) so
 * the two never drift apart or trust the client's clock (design.md Decisions 2/3). Convention:
 * {@code 0=Domingo … 6=Sábado} (PRD Seção 8), not {@link DayOfWeek}'s {@code 1=Monday…7=Sunday}.
 */
@Component
public class BusinessClock {

    public static final java.time.ZoneId ZONE = java.time.ZoneId.of("America/Fortaleza");

    private final Clock clock;

    public BusinessClock() {
        this(Clock.system(ZONE));
    }

    /** Package-private: lets tests fix "now" without touching the system clock. */
    BusinessClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalTime now() {
        return LocalTime.now(clock);
    }

    public int dayOfWeek() {
        return toPrdDayOfWeek(today().getDayOfWeek());
    }

    public static int toPrdDayOfWeek(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue();
    }
}
