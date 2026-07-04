package com.comanda.orders.status.web;

import com.comanda.orders.status.SetOpenRequest;
import com.comanda.orders.status.TenantStatusResponse;
import com.comanda.orders.status.TenantStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/loja/status")
public class TenantStatusController {

    private final TenantStatusService tenantStatusService;

    public TenantStatusController(TenantStatusService tenantStatusService) {
        this.tenantStatusService = tenantStatusService;
    }

    @GetMapping
    public TenantStatusResponse get() {
        return tenantStatusService.get();
    }

    @PatchMapping
    public TenantStatusResponse setOpen(@Valid @RequestBody SetOpenRequest request) {
        return tenantStatusService.setOpen(request.open());
    }
}
