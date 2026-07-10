package com.comanda.onboarding.web;

import com.comanda.menu.CategoryNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.plans.PlanLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Scoped to {@code com.comanda.onboarding} controllers. Re-handles {@code menu-management}'s
 * not-found exceptions and {@code plans}' limit exception for the same reason {@code
 * OrdersExceptionHandler} does: {@code @RestControllerAdvice}'s {@code basePackages} matches on
 * the controller's package, not the exception's origin.
 */
@RestControllerAdvice(basePackages = "com.comanda.onboarding")
public class OnboardingExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound() {
        return error(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Categoria não encontrada.");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound() {
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Produto não encontrado.");
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handlePlanLimitExceeded(PlanLimitExceededException e) {
        return error(HttpStatus.PAYMENT_REQUIRED, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Dados inválidos.");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
