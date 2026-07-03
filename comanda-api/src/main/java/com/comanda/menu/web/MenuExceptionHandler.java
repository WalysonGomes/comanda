package com.comanda.menu.web;

import com.comanda.menu.AdditionalGroupNotFoundException;
import com.comanda.menu.AdditionalItemNotFoundException;
import com.comanda.menu.CategoryNotEmptyException;
import com.comanda.menu.CategoryNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.menu.UnsupportedImageTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.comanda.menu")
public class MenuExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound() {
        return error(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Categoria não encontrada.");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound() {
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Produto não encontrado.");
    }

    @ExceptionHandler(AdditionalGroupNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAdditionalGroupNotFound() {
        return error(HttpStatus.NOT_FOUND, "ADDITIONAL_GROUP_NOT_FOUND", "Grupo de adicionais não encontrado.");
    }

    @ExceptionHandler(AdditionalItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAdditionalItemNotFound() {
        return error(HttpStatus.NOT_FOUND, "ADDITIONAL_ITEM_NOT_FOUND", "Item de adicional não encontrado.");
    }

    @ExceptionHandler(CategoryNotEmptyException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotEmpty(CategoryNotEmptyException e) {
        return error(HttpStatus.CONFLICT, "CATEGORY_NOT_EMPTY",
                "Categoria possui " + e.getProductCount() + " produto(s). Mova ou remova os produtos antes.");
    }

    @ExceptionHandler(UnsupportedImageTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedImageType() {
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_TYPE", "Formato de imagem não suportado. Use JPEG, PNG ou WebP.");
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
