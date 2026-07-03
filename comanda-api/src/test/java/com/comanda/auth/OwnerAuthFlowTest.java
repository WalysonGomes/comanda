package com.comanda.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.comanda.platform.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end coverage of the owner-auth spec (tasks 6.1-6.4): atomic signup with rollback on
 * conflict, anti-enumeration login, refresh rotation with reuse detection, and the tenant claim
 * resolving/isolating through the existing {@code JwtTenantResolver} (foundations).
 * {@link PainelEchoController} is test-only instrumentation, not production surface.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(
        classes = {ComandaApiApplication.class, OwnerAuthFlowTest.PainelEchoController.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OwnerAuthFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void signupCreatesTenantAndOwnerAtomicallyAndStartsSession() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("Ana", "Brasa Burger", "brasa-burger", "85999990000", "ana@example.com", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        assertThat(count("tenants", "subdomain = 'brasa-burger'")).isEqualTo(1);
        assertThat(count("users", "email = 'ana@example.com'")).isEqualTo(1);
    }

    @Test
    void signupWithDuplicateSubdomainIsRejectedAndPersistsNothingNew() throws Exception {
        jdbcTemplate.update("INSERT INTO tenants (subdomain, name) VALUES (?, ?)", "acme", "Acme");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("Bob", "Acme 2", "acme", "85999990001", "bob@example.com", "senha123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBDOMAIN_ALREADY_IN_USE"));

        assertThat(count("users", "email = 'bob@example.com'")).isZero();
    }

    @Test
    void signupWithDuplicateEmailIsRejectedAndPersistsNothingNew() throws Exception {
        signup("Carla", "Carla Doces", "carla-doces", "85999990002", "carla@example.com", "senha123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("Carla 2", "Carla Doces 2", "carla-doces-2", "85999990003", "carla@example.com", "outrasenha")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        assertThat(count("tenants", "subdomain = 'carla-doces-2'")).isZero();
    }

    @Test
    void loginWithValidCredentialsIssuesTokens() throws Exception {
        signup("Dora", "Dora Marmitas", "dora-marmitas", "85999990004", "dora@example.com", "senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("dora@example.com", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void loginFailuresAreIndistinguishable() throws Exception {
        signup("Eva", "Eva Acai", "eva-acai", "85999990005", "eva@example.com", "senha123");
        signup("Gil", "Gil Salgados", "gil-salgados", "85999990006", "gil@example.com", "senha123");
        jdbcTemplate.update("UPDATE users SET is_active = false WHERE email = 'gil@example.com'");

        String wrongPasswordBody = attemptLogin("eva@example.com", "senha-errada");
        String noSuchEmailBody = attemptLogin("nao-existe@example.com", "qualquer");
        String inactiveAccountBody = attemptLogin("gil@example.com", "senha123");

        assertThat(wrongPasswordBody).isEqualTo(noSuchEmailBody).isEqualTo(inactiveAccountBody);
    }

    @Test
    void refreshRotatesTokenAndRejectsReuseOfThePreviousOne() throws Exception {
        MvcResult signupResult = signup("Hugo", "Hugo Burger", "hugo-burger", "85999990007", "hugo@example.com", "senha123");
        Cookie cookieA = signupResult.getResponse().getCookie("refresh_token");
        assertThat(cookieA).isNotNull();

        MvcResult firstRefresh = mockMvc.perform(post("/api/auth/refresh").cookie(cookieA))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookieB = firstRefresh.getResponse().getCookie("refresh_token");
        assertThat(cookieB).isNotNull();
        assertThat(cookieB.getValue()).isNotEqualTo(cookieA.getValue());

        mockMvc.perform(post("/api/auth/refresh").cookie(cookieA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/refresh").cookie(cookieB))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenClaimsTenantAndIsolatesFromOtherTenants() throws Exception {
        MvcResult signupA = signup("Iris", "Iris Cafe", "iris-cafe", "85999990008", "iris@example.com", "senha123");
        MvcResult signupB = signup("Joao", "Joao Grill", "joao-grill", "85999990009", "joao@example.com", "senha123");

        String tokenA = accessTokenOf(signupA);
        String tokenB = accessTokenOf(signupB);
        Long tenantIdA = jdbcTemplate.queryForObject("SELECT id FROM tenants WHERE subdomain = 'iris-cafe'", Long.class);
        Long tenantIdB = jdbcTemplate.queryForObject("SELECT id FROM tenants WHERE subdomain = 'joao-grill'", Long.class);
        assertThat(tenantIdA).isNotEqualTo(tenantIdB);

        mockMvc.perform(get("/api/painel/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"tenantId\":" + tenantIdA + "}"));

        mockMvc.perform(get("/api/painel/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"tenantId\":" + tenantIdB + "}"));
    }

    private MvcResult signup(
            String name, String businessName, String subdomain, String whatsapp, String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(name, businessName, subdomain, whatsapp, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String attemptLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String accessTokenOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private int count(String table, String where) {
        Integer value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Integer.class);
        return value == null ? 0 : value;
    }

    private String signupJson(
            String name, String businessName, String subdomain, String whatsapp, String email, String password) {
        return """
                {"name":"%s","businessName":"%s","subdomain":"%s","whatsappNumber":"%s","email":"%s","password":"%s"}
                """
                .formatted(name, businessName, subdomain, whatsapp, email, password);
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """
                .formatted(email, password);
    }

    @RestController
    static class PainelEchoController {

        @GetMapping("/api/painel/ping")
        Map<String, Object> painelPing() {
            return Map.of("tenantId", TenantContext.get());
        }
    }
}
