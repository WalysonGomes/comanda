package com.comanda.platform.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Baseline security (PRD 7.1) plus the transversal hardening from {@code reliability-and-security}
 * (PRD Seção 9): stateless, no CSRF (no cookie-based session to forge — the refresh cookie is
 * validated by an explicit rotation check, not by Spring Security session state), no default
 * login page/basic auth. Gating of {@code /api/painel/**} (owner-auth's JWT access tokens
 * required) vs. the public routes ({@code /api/auth/**} signup/login/refresh, {@code /api/loja/**}
 * storefront) is delegated to {@link com.comanda.platform.tenancy.TenantResolutionFilter}, which
 * already resolves and rejects by JWT/subdomain before any handler runs — so authorization stays
 * permissive here.
 *
 * <p>Security headers (task 2.1): HSTS/X-Content-Type-Options/frame-ancestors/Referrer-Policy/CSP
 * are emitted by the app, not Caddy (design.md Decision 1) — Caddy only terminates TLS. The CSP is
 * deliberately narrow (task 2.2): {@code 'self'} for scripts/connections, plus the two Google
 * Fonts hosts the SPA's {@code index.html} loads (see {@code style-src}/{@code font-src}) — no
 * {@code unsafe-inline} for scripts, since the Vite build emits only external module scripts.
 *
 * <p>CORS (task 2.3) is restricted to the app domain and its tenant subdomains — in production the
 * SPA is same-origin with the API (PRD 7.3: backend serves the static build), so this mostly
 * defends against a third-party page issuing credentialed cross-origin requests; the scheme
 * wildcard lets it work for both {@code http://} in local dev and {@code https://} in prod without
 * hardcoding localhost.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CSP = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self' https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data: blob:; "
            + "connect-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    private final String appDomain;

    public SecurityConfig(@Value("${app.domain}") String appDomain) {
        this.appDomain = appDomain;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP)));
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*://" + appDomain, "*://*." + appDomain));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
