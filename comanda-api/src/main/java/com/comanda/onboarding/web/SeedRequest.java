package com.comanda.onboarding.web;

import com.comanda.onboarding.Segment;
import jakarta.validation.constraints.NotNull;

public record SeedRequest(@NotNull(message = "Segmento é obrigatório.") Segment segment) {
}
