package com.comanda.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.comanda.ComandaApiApplication;
import com.comanda.TestDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers tasks 6.1-6.2 (PRD Seção 9, Regra 14): runs signup, login (success and failure), refresh
 * and order creation — the flows the design.md audit flags as sensitive — while capturing every
 * log event emitted, then asserts none of them contain the raw password, the JWT access/refresh
 * tokens, or a phone number used in the run. The audit in {@code src/main} (task 1.3) found no
 * logging at all in these flows today, so this is a regression guard: it fails the moment someone
 * adds a {@code log.info(request)} or similar without masking first.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class LogSafetyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final String PASSWORD = "senha-super-secreta-123";
    private static final String OWNER_PHONE = "85999911122";
    private static final String CUSTOMER_PHONE = "85988877766";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sensitiveFlowsNeverLeakPasswordTokenOrPhone() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);

        String accessToken;
        String refreshTokenValue;
        try {
            MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Lia","businessName":"Lia Doces","subdomain":"log-safety-tenant",
                                     "whatsappNumber":"%s","email":"lia-logsafety@example.com","password":"%s"}
                                    """.formatted(OWNER_PHONE, PASSWORD)))
                    .andExpect(status().isCreated())
                    .andReturn();
            accessToken = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                    .get("accessToken").asText();
            Cookie refreshCookie = signupResult.getResponse().getCookie("refresh_token");
            refreshTokenValue = refreshCookie.getValue();

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"lia-logsafety@example.com\",\"password\":\"senha-errada\"}"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"lia-logsafety@example.com\",\"password\":\"" + PASSWORD + "\"}"));

            mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie));

            Long categoryId = objectMapper
                    .readTree(mockMvc.perform(post("/api/painel/categories")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"name\":\"Doces\"}"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString())
                    .get("id")
                    .asLong();
            Long productId = objectMapper
                    .readTree(mockMvc.perform(post("/api/painel/products")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"name\":\"Brigadeiro\",\"description\":\"\",\"price\":\"3.00\",\"categoryId\":"
                                            + categoryId + "}"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString())
                    .get("id")
                    .asLong();

            mockMvc.perform(MockMvcRequestBuilders.post("/api/loja/pedidos")
                    .with(request -> {
                        request.setServerName(TestDomain.host("log-safety-tenant"));
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"idempotencyKey":"%s","customerName":"Cliente Log","customerPhone":"%s",
                             "deliveryType":"RETIRADA","notes":"","lines":[{"productId":%d,"quantity":1,"additionalItemIds":[]}]}
                            """.formatted(java.util.UUID.randomUUID(), CUSTOMER_PHONE, productId)));
        } finally {
            rootLogger.detachAppender(appender);
        }

        String allLogOutput = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);

        assertThat(allLogOutput)
                .doesNotContain(PASSWORD)
                .doesNotContain("senha-errada")
                .doesNotContain(accessToken)
                .doesNotContain(refreshTokenValue)
                .doesNotContain(OWNER_PHONE)
                .doesNotContain(CUSTOMER_PHONE);
    }
}
