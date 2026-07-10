package com.comanda.plans.web;

import com.comanda.plans.PlanStatusResponse;
import com.comanda.plans.PlanStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/plans/status")
public class PlanStatusController {

    private final PlanStatusService planStatusService;

    public PlanStatusController(PlanStatusService planStatusService) {
        this.planStatusService = planStatusService;
    }

    @GetMapping
    public PlanStatusResponse get() {
        return planStatusService.get();
    }
}
