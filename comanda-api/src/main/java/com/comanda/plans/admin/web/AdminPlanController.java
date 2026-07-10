package com.comanda.plans.admin.web;

import com.comanda.plans.admin.PlanActivationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator-only manual billing (PRD Seção 10, task 4.1): {@code /api/admin/**} is excluded from
 * tenant resolution ({@link com.comanda.platform.tenancy.TenantResolutionFilter}) and gated by
 * {@link com.comanda.platform.security.AdminAuthFilter} instead — a bearer token, never the
 * OWNER's JWT, so no endpoint reachable with a tenant's own session can elevate its plan.
 */
@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController {

    private final PlanActivationService planActivationService;

    public AdminPlanController(PlanActivationService planActivationService) {
        this.planActivationService = planActivationService;
    }

    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<ActivationResponse> activate(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ActivationResponse.from(planActivationService.activateEssencial(tenantId)));
    }

    @PostMapping("/{tenantId}/downgrade")
    public ResponseEntity<ActivationResponse> downgrade(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ActivationResponse.from(planActivationService.downgradeToGratuito(tenantId)));
    }
}
