package com.comanda.auth.web;

import com.comanda.auth.AuthenticationService;
import com.comanda.auth.InvalidRefreshTokenException;
import com.comanda.auth.SignupService;
import com.comanda.auth.security.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public signup/login/refresh (D6). Excluded from tenant resolution by
 * {@code TenantResolutionFilter} — none of these routes have a tenant yet when they're called.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final SignupService signupService;
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            SignupService signupService,
            AuthenticationService authenticationService,
            RefreshTokenService refreshTokenService) {
        this.signupService = signupService;
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        RefreshTokenService.Session session = signupService.signup(request.toCommand());
        return sessionResponse(HttpStatus.CREATED, session);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        RefreshTokenService.Session session = authenticationService.login(request.email(), request.password());
        return sessionResponse(HttpStatus.OK, session);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenService.Session session = refreshTokenService.rotate(refreshToken);
        return sessionResponse(HttpStatus.OK, session);
    }

    private ResponseEntity<AuthResponse> sessionResponse(HttpStatus status, RefreshTokenService.Session session) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenService.refreshTtl())
                .build();

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthResponse.of(session.user(), session.accessToken()));
    }
}
