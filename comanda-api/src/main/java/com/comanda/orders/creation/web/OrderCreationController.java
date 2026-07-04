package com.comanda.orders.creation.web;

import com.comanda.orders.creation.OrderCreationOutcome;
import com.comanda.orders.creation.OrderCreationRequest;
import com.comanda.orders.creation.OrderCreationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, subdomain-resolved (no JWT) — {@code /api/loja/**} per {@code
 * TenantResolutionFilter}/{@code SecurityConfig}'s existing convention for the storefront prefix.
 */
@RestController
@RequestMapping("/api/loja/orders")
public class OrderCreationController {

    private final OrderCreationService orderCreationService;

    public OrderCreationController(OrderCreationService orderCreationService) {
        this.orderCreationService = orderCreationService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OrderCreationRequest request) {
        OrderCreationOutcome outcome = orderCreationService.create(request);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.order());
    }
}
