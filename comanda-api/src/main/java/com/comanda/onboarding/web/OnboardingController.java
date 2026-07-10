package com.comanda.onboarding.web;

import com.comanda.onboarding.hours.OnboardingBusinessHoursService;
import com.comanda.onboarding.hours.OnboardingBusinessHoursService.DayHours;
import com.comanda.onboarding.seed.OnboardingSeedService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin endpoints for onboarding wizard steps 0/2 (task 5.2/5.3) — both reuse existing services
 * (`menu-management`'s creation services, `public-storefront`'s {@code BusinessHoursRepository});
 * no cardápio or horário rule is reimplemented here. Behind {@code /api/painel/**} (JWT), called
 * right after the account step (step 1) with the token {@code owner-auth}'s signup just issued.
 */
@RestController
@RequestMapping("/api/painel/onboarding")
public class OnboardingController {

    private final OnboardingSeedService seedService;
    private final OnboardingBusinessHoursService businessHoursService;

    public OnboardingController(OnboardingSeedService seedService, OnboardingBusinessHoursService businessHoursService) {
        this.seedService = seedService;
        this.businessHoursService = businessHoursService;
    }

    @PostMapping("/seed")
    public ResponseEntity<Void> seed(@Valid @RequestBody SeedRequest request) {
        seedService.seed(request.segment());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/business-hours")
    public ResponseEntity<Void> businessHours(@Valid @RequestBody BusinessHoursRequest request) {
        businessHoursService.save(request.rows().stream()
                .map(row -> new DayHours(row.dayOfWeek(), row.opensAt(), row.closesAt(), row.closed()))
                .toList());
        return ResponseEntity.noContent().build();
    }
}
