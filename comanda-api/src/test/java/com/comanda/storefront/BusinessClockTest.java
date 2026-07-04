package com.comanda.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * Task 1.4 / 5.2 "day boundary": the PRD's {@code 0=Dom…6=Sáb} convention is the single riskiest
 * off-by-one in the whole slice (Seção 13 flags it explicitly) — exercised here for all 7 days
 * against a fixed clock, no Spring context needed.
 */
class BusinessClockTest {

    // 2024-01-01 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);

    @Test
    void mapsEachWeekdayToThePrdConvention() {
        assertThat(dayOfWeekAt(MONDAY.plusDays(6))).isEqualTo(0); // Sunday
        assertThat(dayOfWeekAt(MONDAY)).isEqualTo(1); // Monday
        assertThat(dayOfWeekAt(MONDAY.plusDays(1))).isEqualTo(2); // Tuesday
        assertThat(dayOfWeekAt(MONDAY.plusDays(2))).isEqualTo(3); // Wednesday
        assertThat(dayOfWeekAt(MONDAY.plusDays(3))).isEqualTo(4); // Thursday
        assertThat(dayOfWeekAt(MONDAY.plusDays(4))).isEqualTo(5); // Friday
        assertThat(dayOfWeekAt(MONDAY.plusDays(5))).isEqualTo(6); // Saturday
    }

    @Test
    void todayAndNowReadTheInjectedClockInTheFixedZone() {
        LocalDateTime local = LocalDateTime.of(MONDAY, LocalTime.of(21, 30));
        BusinessClock clock = clockAt(local);

        assertThat(clock.today()).isEqualTo(MONDAY);
        assertThat(clock.now()).isEqualTo(LocalTime.of(21, 30));
    }

    private int dayOfWeekAt(LocalDate date) {
        return clockAt(LocalDateTime.of(date, LocalTime.NOON)).dayOfWeek();
    }

    private BusinessClock clockAt(LocalDateTime localDateTime) {
        ZonedDateTime zoned = localDateTime.atZone(BusinessClock.ZONE);
        return new BusinessClock(Clock.fixed(zoned.toInstant(), BusinessClock.ZONE));
    }
}
