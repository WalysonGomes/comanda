package com.comanda.plans.admin.web;

import com.comanda.plans.admin.AdminTenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.comanda.plans.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(AdminTenantNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse("TENANT_NOT_FOUND", "Tenant não encontrado."));
    }
}
