package com.comanda.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.comanda.ComandaApiApplication;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PRD 7.1 / spec `platform-runtime`: requests are served by Virtual Threads and the application
 * timezone is fixed to America/Fortaleza. {@link ThreadProbeController} is test-only
 * instrumentation, not production surface.
 */
@Testcontainers
@SpringBootTest(
        classes = {ComandaApiApplication.class, RuntimeConfigurationTest.ThreadProbeController.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RuntimeConfigurationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Test
    void applicationTimezoneIsFixedToFortaleza() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Fortaleza");
    }

    @Test
    void requestsAreHandledByAVirtualThread() {
        RestTemplate restTemplate = new RestTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTemplate.getForObject(
                "http://localhost:" + port + "/internal/test/thread-probe", Map.class);

        assertThat(body).containsEntry("virtual", true);
    }

    @RestController
    static class ThreadProbeController {

        @GetMapping("/internal/test/thread-probe")
        Map<String, Object> probe() {
            return Map.of("virtual", Thread.currentThread().isVirtual());
        }
    }
}
