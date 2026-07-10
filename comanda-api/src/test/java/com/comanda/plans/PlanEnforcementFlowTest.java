package com.comanda.plans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers tasks 10.1-10.5 and 10.7: Gratuito's product/category/order-per-month limits (402 with
 * the right {@code code}, Essencial never blocked), the idempotent-retry-doesn't-recount
 * guarantee, the lazy monthly reset, the 25/30 quota banner threshold, plan-based retention as a
 * read filter (never delete), and the onboarding demo seed. Signs up real tenants through {@code
 * owner-auth}, exactly like the other flow tests. Admin activation (task 10.6) is covered
 * separately in {@link PlanActivationAdminTest}, which needs its own {@code app.admin.token}.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PlanEnforcementFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- 10.1 limite de produtos / categorias ----------

    @Test
    void thirtyFirstProductIsBlockedForGratuito_essencialUnlimited() throws Exception {
        Tenant tenant = signupTenant("prod-limit-tenant");
        Long categoryId = createCategory(tenant.token, "Categoria única").get("id").asLong();

        for (int i = 0; i < 30; i++) {
            createProduct(tenant.token, categoryId, "Produto " + i, "10.00")
                    .andExpect(status().isCreated());
        }
        createProduct(tenant.token, categoryId, "Produto 31", "10.00")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PRODUCT_LIMIT_REACHED"));
        assertThat(count("products", "tenant_id = " + tenant.id)).isEqualTo(30);

        setPlan(tenant.id, "ESSENCIAL");
        createProduct(tenant.token, categoryId, "Produto 31 essencial", "10.00")
                .andExpect(status().isCreated());
    }

    @Test
    void sixthCategoryIsBlockedForGratuito_essencialUnlimited() throws Exception {
        Tenant tenant = signupTenant("cat-limit-tenant");
        for (int i = 0; i < 5; i++) {
            createCategory(tenant.token, "Categoria " + i).get("id");
        }
        mockMvc.perform(post("/api/painel/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Categoria 6\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("CATEGORY_LIMIT_REACHED"));
        assertThat(count("categories", "tenant_id = " + tenant.id)).isEqualTo(5);

        setPlan(tenant.id, "ESSENCIAL");
        createCategory(tenant.token, "Categoria 6 essencial").get("id").asLong();
    }

    // ---------- 10.2 limite de pedidos / retry idempotente ----------

    @Test
    void thirtyFirstOrderOfMonthIsBlocked_retryOfThirtiethDoesNotDoubleCount_essencialUnlimited() throws Exception {
        Tenant tenant = signupTenant("order-limit-tenant");
        Long categoryId = createCategory(tenant.token, "Cardápio").get("id").asLong();
        Long productId = objectMapper.readTree(createProductReturning(tenant.token, categoryId, "Marmita", "10.00")).get("id").asLong();

        String lastKey = null;
        for (int i = 0; i < 30; i++) {
            lastKey = UUID.randomUUID().toString();
            createOrder(tenant.subdomain, orderJson(lastKey, productId, 1)).andExpect(status().isCreated());
        }
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(30);

        // Retry of the 30th order's idempotency key must not recount.
        createOrder(tenant.subdomain, orderJson(lastKey, productId, 1)).andExpect(status().isOk());
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(30);

        // A genuinely new (31st) order is blocked.
        createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT_REACHED"));
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(30);

        setPlan(tenant.id, "ESSENCIAL");
        createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1)).andExpect(status().isCreated());
    }

    // ---------- 10.3 reset mensal ----------

    @Test
    void firstOrderOfNewPeriodIsNotBlockedAfterStaleCompetency() throws Exception {
        Tenant tenant = signupTenant("reset-tenant");
        Long categoryId = createCategory(tenant.token, "Cardápio").get("id").asLong();
        Long productId = objectMapper.readTree(createProductReturning(tenant.token, categoryId, "Marmita", "10.00")).get("id").asLong();

        jdbcTemplate.update(
                "UPDATE tenants SET order_count_month = 30, order_count_month_period = '2020-01' WHERE id = ?", tenant.id);

        createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1)).andExpect(status().isCreated());
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(1);
    }

    // ---------- 10.4 aviso de cota ----------

    @Test
    void quotaWarningAppearsAt25AndAbove_notBelow() throws Exception {
        Tenant tenant = signupTenant("quota-tenant");

        jdbcTemplate.update("UPDATE tenants SET order_count_month = 24 WHERE id = ?", tenant.id);
        statusOf(tenant.token).andExpect(jsonPath("$.showQuotaWarning").value(false));

        jdbcTemplate.update("UPDATE tenants SET order_count_month = 26 WHERE id = ?", tenant.id);
        statusOf(tenant.token)
                .andExpect(jsonPath("$.showQuotaWarning").value(true))
                .andExpect(jsonPath("$.orderCountMonth").value(26));

        setPlan(tenant.id, "ESSENCIAL");
        statusOf(tenant.token).andExpect(jsonPath("$.showQuotaWarning").value(false));
    }

    // ---------- 10.5 retenção por plano ----------

    @Test
    void retentionFiltersByPlan_neverDeletes_elevatingReexposesOldOrders() throws Exception {
        Tenant tenant = signupTenant("retention-tenant");
        Long categoryId = createCategory(tenant.token, "Cardápio").get("id").asLong();
        Long productId = objectMapper.readTree(createProductReturning(tenant.token, categoryId, "Marmita", "10.00")).get("id").asLong();

        long recentOrderId = objectMapper.readTree(
                        createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString())
                .get("id").asLong();
        long oldOrderId = objectMapper.readTree(
                        createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString())
                .get("id").asLong();
        jdbcTemplate.update("UPDATE orders SET created_at = now() - interval '20 days' WHERE id = ?", oldOrderId);

        mockMvc.perform(get("/api/painel/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[?(@.id == " + oldOrderId + ")]").isEmpty())
                .andExpect(jsonPath("$.orders[?(@.id == " + recentOrderId + ")]").isNotEmpty());

        mockMvc.perform(get("/api/painel/orders/" + oldOrderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token))
                .andExpect(status().isNotFound());

        assertThat(count("orders", "id = " + oldOrderId)).isEqualTo(1); // never deleted

        setPlan(tenant.id, "ESSENCIAL");
        mockMvc.perform(get("/api/painel/orders/" + oldOrderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token))
                .andExpect(status().isOk());
    }

    // ---------- 10.7 seed de demonstração ----------

    @Test
    void demoSeedRespectsLimitsAndIsEditable() throws Exception {
        Tenant tenant = signupTenant("seed-tenant");

        mockMvc.perform(post("/api/painel/onboarding/seed")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segment\":\"HAMBURGUERIA\"}"))
                .andExpect(status().isNoContent());

        long categoryCount = count("categories", "tenant_id = " + tenant.id);
        long productCount = count("products", "tenant_id = " + tenant.id);
        assertThat(categoryCount).isBetween(1L, 5L);
        assertThat(productCount).isBetween(1L, 30L);

        MvcResult productsResult = mockMvc.perform(get("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode products = objectMapper.readTree(productsResult.getResponse().getContentAsString());
        assertThat(products.size()).isGreaterThan(0);
        long firstProductId = products.get(0).get("id").asLong();
        long firstCategoryId = products.get(0).get("categoryId").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/painel/products/" + firstProductId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Editado pelo dono\",\"description\":\"novo\",\"price\":\"99.00\",\"categoryId\":" + firstCategoryId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Editado pelo dono"));
    }

    // ---------- helpers ----------

    private record Tenant(Long id, String subdomain, String token) {
    }

    private Tenant signupTenant(String subdomain) throws Exception {
        String email = subdomain + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dono","businessName":"Negocio","subdomain":"%s","whatsappNumber":"85999990000","email":"%s","password":"senha123"}
                                """.formatted(subdomain, email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("accessToken").asText();
        Long tenantId = json.get("user").get("tenantId").asLong();
        return new Tenant(tenantId, subdomain, token);
    }

    private JsonNode createCategory(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions createProduct(String token, Long categoryId, String name, String price) throws Exception {
        return mockMvc.perform(post("/api/painel/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson(categoryId, name, price)));
    }

    private String createProductReturning(String token, Long categoryId, String name, String price) throws Exception {
        return mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, name, price)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String productJson(Long categoryId, String name, String price) {
        return "{\"name\":\"" + name + "\",\"description\":\"desc\",\"price\":\"" + price + "\",\"categoryId\":" + categoryId + "}";
    }

    private org.springframework.test.web.servlet.ResultActions createOrder(String subdomain, String body) throws Exception {
        return mockMvc.perform(withSubdomain(post("/api/loja/pedidos"), subdomain)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions statusOf(String token) throws Exception {
        return mockMvc.perform(get("/api/painel/plans/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withSubdomain(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String subdomain) {
        return builder.with(request -> {
            request.setServerName(subdomain + ".comanda.local");
            return request;
        });
    }

    private String orderJson(String idempotencyKey, Long productId, int quantity) {
        return """
                {"idempotencyKey":"%s","customerName":"Cliente Teste","customerPhone":"85988887777",
                 "deliveryType":"RETIRADA","notes":"","lines":[{"productId":%d,"quantity":%d,"additionalItemIds":[]}]}
                """.formatted(idempotencyKey, productId, quantity);
    }

    private void setPlan(Long tenantId, String plan) {
        jdbcTemplate.update("UPDATE tenants SET plan = ? WHERE id = ?", plan, tenantId);
    }

    private long count(String table, String where) {
        Long value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Long.class);
        return value == null ? 0 : value;
    }

    private int tenantOrderCount(Long tenantId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT order_count_month FROM tenants WHERE id = ?", Integer.class, tenantId);
        return value == null ? 0 : value;
    }
}
