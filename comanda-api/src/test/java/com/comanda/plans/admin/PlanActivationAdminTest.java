package com.comanda.plans.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Task 10.6: manual activation/downgrade reflects in {@code subscriptions}/{@code tenants} with
 * {@code external_subscription_id} null, and no OWNER-reachable endpoint elevates a plan. Uses
 * its own {@link TestPropertySource} for {@code app.admin.token} — the default (empty) config
 * always rejects, so this needs a dedicated context, same reasoning as {@code AuthRateLimitingTest}.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "app.admin.token=test-operator-token")
class PlanActivationAdminTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void operatorActivatesAndDowngradesEssencial_externalSubscriptionIdStaysNull() throws Exception {
        Long tenantId = signupTenant("admin-activation-tenant");

        MvcResult activate = mockMvc.perform(post("/api/admin/plans/" + tenantId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("ESSENCIAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        JsonNode activated = objectMapper.readTree(activate.getResponse().getContentAsString());
        assertThat(activated.get("currentPeriodEnd").asText()).isNotBlank();

        assertThat(jdbcTemplate.queryForObject("SELECT plan FROM tenants WHERE id = ?", String.class, tenantId))
                .isEqualTo("ESSENCIAL");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT external_subscription_id FROM subscriptions WHERE tenant_id = ?", String.class, tenantId))
                .isNull();

        mockMvc.perform(post("/api/admin/plans/" + tenantId + "/downgrade")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("GRATUITO"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(jdbcTemplate.queryForObject("SELECT plan FROM tenants WHERE id = ?", String.class, tenantId))
                .isEqualTo("GRATUITO");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT external_subscription_id FROM subscriptions WHERE tenant_id = ?", String.class, tenantId))
                .isNull();
    }

    @Test
    void ownerJwtCannotActivateOwnPlan() throws Exception {
        String email = "owner-cannot-elevate@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dono","businessName":"Negocio","subdomain":"owner-cannot-elevate","whatsappNumber":"85999990000","email":"%s","password":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String ownerToken = json.get("accessToken").asText();
        Long tenantId = json.get("user").get("tenantId").asLong();

        mockMvc.perform(post("/api/admin/plans/" + tenantId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isUnauthorized());

        assertThat(jdbcTemplate.queryForObject("SELECT plan FROM tenants WHERE id = ?", String.class, tenantId))
                .isEqualTo("GRATUITO");
    }

    @Test
    void missingOrWrongTokenIsRejected() throws Exception {
        Long tenantId = signupTenant("admin-no-token-tenant");

        mockMvc.perform(post("/api/admin/plans/" + tenantId + "/activate"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/plans/" + tenantId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    private Long signupTenant(String subdomain) throws Exception {
        String email = subdomain + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dono","businessName":"Negocio","subdomain":"%s","whatsappNumber":"85999990000","email":"%s","password":"senha123"}
                                """.formatted(subdomain, email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("user").get("tenantId").asLong();
    }
}
