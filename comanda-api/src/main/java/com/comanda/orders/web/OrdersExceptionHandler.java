package com.comanda.orders.web;

import com.comanda.menu.AdditionalItemNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.orders.InvalidCancellationReasonException;
import com.comanda.orders.InvalidStatusTransitionException;
import com.comanda.orders.ItemUnavailableException;
import com.comanda.orders.OrderNotFoundException;
import com.comanda.plans.PlanLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Scoped to {@code com.comanda.orders} controllers (both the public creation endpoint and the
 * panel endpoints). Reuses {@code menu-management}'s not-found exceptions for product/additional
 * lookups during creation, since order-operation is just another caller of the same cardápio
 * data — no need for a duplicate exception type.
 */
@RestControllerAdvice(basePackages = "com.comanda.orders")
public class OrdersExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound() {
        return error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Pedido não encontrado.");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound() {
        return error(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_FOUND", "Produto não encontrado.");
    }

    @ExceptionHandler(AdditionalItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAdditionalItemNotFound() {
        return error(HttpStatus.BAD_REQUEST, "ADDITIONAL_ITEM_NOT_FOUND", "Adicional não encontrado.");
    }

    @ExceptionHandler(ItemUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleItemUnavailable(ItemUnavailableException e) {
        return error(HttpStatus.CONFLICT, "ITEM_UNAVAILABLE", e.getItemName() + " não está mais disponível.");
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition() {
        return error(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", "Transição de status inválida.");
    }

    @ExceptionHandler(InvalidCancellationReasonException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCancellationReason() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_CANCELLATION_REASON", "Motivo do cancelamento deve ter ao menos 10 caracteres.");
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
