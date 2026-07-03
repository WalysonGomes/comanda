package com.comanda.auth.web;

import com.comanda.auth.EmailAlreadyExistsException;
import com.comanda.auth.InvalidCredentialsException;
import com.comanda.auth.InvalidRefreshTokenException;
import com.comanda.auth.SubdomainAlreadyInUseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists() {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Este e-mail já está cadastrado.");
    }

    @ExceptionHandler(SubdomainAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleSubdomainAlreadyInUse() {
        return error(HttpStatus.CONFLICT, "SUBDOMAIN_ALREADY_IN_USE", "Este endereço já está em uso.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha incorretos.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Sessão expirada. Faça login novamente.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
