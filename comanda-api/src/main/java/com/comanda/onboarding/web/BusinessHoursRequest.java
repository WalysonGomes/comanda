package com.comanda.onboarding.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record BusinessHoursRequest(@NotNull @Valid List<Row> rows) {

    public record Row(
            @Min(0) @Max(6) short dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closed) {
    }
}
