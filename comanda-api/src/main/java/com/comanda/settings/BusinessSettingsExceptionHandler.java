package com.comanda.settings;

import com.comanda.auth.SubdomainAlreadyInUseException;
import com.comanda.menu.UnsupportedImageTypeException;
import com.comanda.auth.web.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = BusinessSettingsController.class)
public class BusinessSettingsExceptionHandler {
    @ExceptionHandler(SubdomainAlreadyInUseException.class)
    ResponseEntity<ApiErrorResponse> conflict() { return error(HttpStatus.CONFLICT, "SUBDOMAIN_ALREADY_IN_USE", "Este endereço já está em uso."); }
    @ExceptionHandler(UnsupportedImageTypeException.class)
    ResponseEntity<ApiErrorResponse> image() { return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_TYPE", "Formato de imagem não suportado. Use JPEG, PNG ou WebP."); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiErrorResponse> validation(Exception e) {
        String message = e instanceof MethodArgumentNotValidException m ? m.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getDefaultMessage()).orElse("Dados inválidos.") : e.getMessage();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus s, String c, String m) { return ResponseEntity.status(s).body(new ApiErrorResponse(c,m)); }
}
