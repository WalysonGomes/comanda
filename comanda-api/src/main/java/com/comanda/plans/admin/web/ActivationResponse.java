package com.comanda.plans.admin.web;

import com.comanda.plans.admin.ActivationOutcome;
import java.time.OffsetDateTime;

public record ActivationResponse(
        Long tenantId, String plan, String status, OffsetDateTime currentPeriodEnd, OffsetDateTime cancelledAt) {

    public static ActivationResponse from(ActivationOutcome outcome) {
        return new ActivationResponse(
                outcome.tenant().getId(),
                outcome.tenant().getPlan(),
                outcome.subscription().getStatus().name(),
                outcome.subscription().getCurrentPeriodEnd(),
                outcome.subscription().getCancelledAt());
    }
}
