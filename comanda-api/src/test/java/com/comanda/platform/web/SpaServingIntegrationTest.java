package com.comanda.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.comanda.ComandaApiApplication;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Task 6.1: validates the single artifact end to end against a real embedded server (the SPA
 * fallback relies on container-level error-page redispatch, which a mock MVC environment does
 * not simulate) — the embedded frontend build is served by the backend, both storefront and
 * owner-panel client routes resolve to the SPA shell, a built asset is served directly, and API
 * routes are never captured by the SPA fallback.
 */
@Testcontainers
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpaServingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void rootRouteServesTheSpaShell() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl("/"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/html");
        assertThat(response.getBody()).contains("id=\"root\"");
    }

    @Test
    void ownerPanelClientRouteIsForwardedToTheSpaShell() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl("/painel/pedidos"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("id=\"root\"");
    }

    @Test
    void builtAssetReferencedByTheShellIsServedDirectly() {
        String html = restTemplate.getForObject(baseUrl("/"), String.class);

        Matcher matcher = Pattern.compile("/assets/index-[^\"]+\\.js").matcher(html);
        assertThat(matcher.find()).as("index.html should reference a built JS asset").isTrue();
        String assetPath = matcher.group();

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(assetPath), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("javascript");
    }

    @Test
    void builtFrontendContainsTheConfiguredRootDomain() {
        String configuredDomain = System.getenv("VITE_ROOT_DOMAINS");
        if (configuredDomain == null || configuredDomain.isBlank()) {
            configuredDomain = System.getenv("APP_DOMAIN");
        }
        assertThat(configuredDomain)
            .as("APP_DOMAIN must be supplied to the integrated production build")
            .isNotBlank();

        String html = restTemplate.getForObject(baseUrl("/"), String.class);
        Matcher matcher = Pattern.compile("/assets/index-[^\"]+\\.js").matcher(html);
        assertThat(matcher.find()).isTrue();

        String javascript = restTemplate.getForObject(baseUrl(matcher.group()), String.class);
        assertThat(javascript).contains(configuredDomain.toLowerCase());
    }

    @Test
    void apiRouteIsNeverCapturedByTheSpaFallback() {
        try {
            restTemplate.getForEntity(baseUrl("/api/rota-inexistente"), String.class);
            org.junit.jupiter.api.Assertions.fail("expected a 404 client error");
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).contains("404");
        }
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
