package com.comanda.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security (PRD 7.1): stateless, no CSRF, no default login page/basic auth. Gating of
 * {@code /api/painel/**} (owner-auth's JWT access tokens required) vs. the public routes
 * ({@code /api/auth/**} signup/login/refresh, {@code /api/loja/**} storefront) is delegated to
 * {@link com.comanda.platform.tenancy.TenantResolutionFilter}, which already resolves and
 * rejects by JWT/subdomain before any handler runs — so authorization stays permissive here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
