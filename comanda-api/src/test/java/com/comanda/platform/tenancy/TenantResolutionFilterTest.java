package com.comanda.platform.tenancy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * D2 / spec `multi-tenancy`: public storefront routes resolve by subdomain, owner-panel routes
 * resolve by JWT, and a request whose tenant can't be resolved never reaches a handler. {@link
 * TenantEchoController} is test-only instrumentation, not production surface.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(
        classes = {ComandaApiApplication.class, TenantResolutionFilterTest.TenantEchoController.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TenantResolutionFilterTest {

    private static final String JWT_SECRET = "comanda-dev-secret-change-me-comanda-dev-secret-change-me";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publicRouteWithKnownSubdomainResolvesTenant() throws Exception {
        Long tenantId = insertTenant("acme");

        mockMvc.perform(get("/api/loja/ping").with(request -> {
                    request.setServerName("acme.comanda.local");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"tenantId\":" + tenantId + "}"));
    }

    @Test
    void publicRouteWithUnknownSubdomainIsRejectedBeforeReachingTheHandler() throws Exception {
        mockMvc.perform(get("/api/loja/ping").with(request -> {
                    request.setServerName("nao-existe.comanda.local");
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerPanelRouteWithValidJwtResolvesTenant() throws Exception {
        String token = jwtFor(42L);

        mockMvc.perform(get("/api/painel/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"tenantId\":42}"));
    }

    @Test
    void ownerPanelRouteWithoutJwtIsRejectedBeforeReachingTheHandler() throws Exception {
        mockMvc.perform(get("/api/painel/ping"))
                .andExpect(status().isUnauthorized());
    }

    private Long insertTenant(String subdomain) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tenants (subdomain, name) VALUES (?, ?) RETURNING id",
                Long.class, subdomain, subdomain);
    }

    private String jwtFor(Long tenantId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim(JwtTenantResolver.TENANT_CLAIM, tenantId)
                .signWith(key)
                .compact();
    }

    @RestController
    static class TenantEchoController {

        @GetMapping("/api/loja/ping")
        Map<String, Object> lojaPing() {
            return Map.of("tenantId", TenantContext.get());
        }

        @GetMapping("/api/painel/ping")
        Map<String, Object> painelPing() {
            return Map.of("tenantId", TenantContext.get());
        }
    }
}
