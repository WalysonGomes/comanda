package com.comanda.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.comanda.ComandaApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers task 2.4 (PRD Seção 9): excessive login attempts are throttled with a generic 429 that
 * doesn't reveal credential validity. Uses its own low capacity via {@link TestPropertySource} —
 * distinct from the default-config context shared by the other flow tests — so the bucket state
 * here is deterministic and isolated.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {"app.rate-limit.auth.capacity=3", "app.rate-limit.auth.window-seconds=60"})
class AuthRateLimitingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void excessiveLoginAttemptsAreRateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nao-existe@example.com\",\"password\":\"x\"}"))
                    .andReturn();
        }

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@example.com\",\"password\":\"x\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"RATE_LIMITED\"");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("INVALID_CREDENTIALS")
                .doesNotContain("existe");
    }
}
