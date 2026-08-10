package com.comanda.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.comanda.TestDomain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers tasks 9.1-9.7: idempotent creation (including concurrent retries), cross-tenant
 * isolation of the idempotency key, duplicate/skip/terminal advance rejection, snapshot
 * immutability against later menu edits, cancellation validation, and IDOR on every panel
 * endpoint. Signs up a real tenant through {@code owner-auth} and seeds the cardápio through
 * {@code menu-management}'s real endpoints, exactly like {@code MenuManagementFlowTest}.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderOperationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- 9.1 idempotência de criação ----------

    @Test
    void duplicateCreationWithSameIdempotencyKeyDoesNotDuplicate() throws Exception {
        Tenant tenant = signupTenant("idem-tenant");
        Long productId = createProduct(tenant.token, "Marmita média", "15.00");
        String key = UUID.randomUUID().toString();

        JsonNode first = createOrder(tenant.subdomain, orderJson(key, productId, 2, List.of()));
        JsonNode second = createOrder(tenant.subdomain, orderJson(key, productId, 2, List.of()));

        assertThat(first.get("id").asLong()).isEqualTo(second.get("id").asLong());
        assertThat(first.get("total").asText()).isEqualTo(second.get("total").asText());
        assertThat(count("orders", "idempotency_key = '" + key + "'")).isEqualTo(1);
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateCreationDoesNotDuplicate() throws Exception {
        Tenant tenant = signupTenant("race-tenant");
        Long productId = createProduct(tenant.token, "Suco de laranja", "5.00");
        String key = UUID.randomUUID().toString();
        String body = orderJson(key, productId, 1, List.of());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Integer>> futures = List.of(
                    pool.submit(() -> raceCreate(tenant.subdomain, body, ready, go)),
                    pool.submit(() -> raceCreate(tenant.subdomain, body, ready, go)));
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            for (var f : futures) {
                assertThat(f.get(10, TimeUnit.SECONDS)).isIn(200, 201);
            }
        } finally {
            pool.shutdown();
        }

        assertThat(count("orders", "idempotency_key = '" + key + "'")).isEqualTo(1);
        assertThat(tenantOrderCount(tenant.id)).isEqualTo(1);
    }

    private int raceCreate(String subdomain, String body, CountDownLatch ready, CountDownLatch go) throws Exception {
        ready.countDown();
        go.await();
        MvcResult result = mockMvc.perform(withSubdomain(post("/api/loja/pedidos"), subdomain)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return result.getResponse().getStatus();
    }

    // ---------- 9.2 isolamento cross-tenant da idempotency_key ----------

    @Test
    void sameIdempotencyKeyAcrossTenantsIsIsolated() throws Exception {
        Tenant tenantA = signupTenant("iso-tenant-a");
        Tenant tenantB = signupTenant("iso-tenant-b");
        Long productA = createProduct(tenantA.token, "Coxinha", "6.00");
        Long productB = createProduct(tenantB.token, "Esfirra", "6.00");
        String sharedKey = UUID.randomUUID().toString();

        JsonNode orderA = createOrder(tenantA.subdomain, orderJson(sharedKey, productA, 1, List.of()));
        JsonNode orderB = createOrder(tenantB.subdomain, orderJson(sharedKey, productB, 1, List.of()));

        assertThat(orderA.get("id").asLong()).isNotEqualTo(orderB.get("id").asLong());
        assertThat(count("orders", "idempotency_key = '" + sharedKey + "'")).isEqualTo(2);
    }

    // ---------- 9.3 avanço idempotente / rejeições ----------

    @Test
    void duplicateAdvanceFromSameOriginIsNoOp() throws Exception {
        Tenant tenant = signupTenant("adv-tenant");
        Long productId = createProduct(tenant.token, "X-Tudo", "20.00");
        long orderId = createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()))
                .get("id").asLong();

        advance(tenant.token, orderId, "RECEBIDO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACEITO"));
        advance(tenant.token, orderId, "RECEBIDO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACEITO"));

        assertThat(count("order_status_history", "order_id = " + orderId)).isEqualTo(2); // RECEBIDO (creation) + ACEITO
    }

    @Test
    void skippingAheadIsRejected() throws Exception {
        Tenant tenant = signupTenant("skip-tenant");
        Long productId = createProduct(tenant.token, "Pizza", "40.00");
        long orderId = createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()))
                .get("id").asLong();

        advance(tenant.token, orderId, "PRONTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void advancingDeliveredOrderIsRejected() throws Exception {
        Tenant tenant = signupTenant("terminal-tenant");
        Long productId = createProduct(tenant.token, "Bolo", "30.00");
        long orderId = createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()))
                .get("id").asLong();

        advance(tenant.token, orderId, "RECEBIDO").andExpect(status().isOk());
        advance(tenant.token, orderId, "ACEITO").andExpect(status().isOk());
        advance(tenant.token, orderId, "EM_PREPARO").andExpect(status().isOk());
        advance(tenant.token, orderId, "PRONTO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));

        advance(tenant.token, orderId, "ENTREGUE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    // ---------- 9.4 snapshots imutáveis ----------

    @Test
    void editingProductAfterOrderDoesNotChangeExistingOrder() throws Exception {
        Tenant tenant = signupTenant("snap-tenant");
        JsonNode category = createCategoryJson(tenant.token, "Marmitas");
        Long categoryId = category.get("id").asLong();
        JsonNode product = createProductJson(tenant.token, categoryId, "Marmita grande", "18.00");
        Long productId = product.get("id").asLong();

        JsonNode order = createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()));
        long orderId = order.get("id").asLong();
        String totalBefore = order.get("total").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/painel/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Marmita renomeada\",\"description\":\"nova\",\"price\":\"99.00\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/painel/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Marmita grande"))
                .andExpect(jsonPath("$.total").value(totalBefore));
    }

    // ---------- 9.5 cancelamento ----------

    @Test
    void cancellationRequiresAtLeastTenCharacters() throws Exception {
        Tenant tenant = signupTenant("cancel-tenant");
        Long productId = createProduct(tenant.token, "Açaí 500ml", "22.00");
        long orderId = createOrder(tenant.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()))
                .get("id").asLong();

        cancel(tenant.token, orderId, "curto")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CANCELLATION_REASON"));

        cancel(tenant.token, orderId, "Cliente desistiu do pedido pelo telefone.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        cancel(tenant.token, orderId, "Segundo motivo bem detalhado aqui.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    // ---------- 9.6 IDOR ----------

    @Test
    void orderOfAnotherTenantIsInaccessible() throws Exception {
        Tenant tenantA = signupTenant("idor-tenant-a");
        Tenant tenantB = signupTenant("idor-tenant-b");
        Long productId = createProduct(tenantA.token, "Torta salgada", "12.00");
        long orderId = createOrder(tenantA.subdomain, orderJson(UUID.randomUUID().toString(), productId, 1, List.of()))
                .get("id").asLong();

        mockMvc.perform(get("/api/painel/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantB.token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        advance(tenantB.token, orderId, "RECEBIDO").andExpect(status().isNotFound());
        cancel(tenantB.token, orderId, "Tentativa de cancelamento indevido aqui.").andExpect(status().isNotFound());

        mockMvc.perform(get("/api/painel/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantB.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", org.hamcrest.Matchers.empty()));
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

    private Long createProduct(String token, String name, String price) throws Exception {
        JsonNode category = createCategoryJson(token, name + " categoria");
        return createProductJson(token, category.get("id").asLong(), name, price).get("id").asLong();
    }

    private JsonNode createCategoryJson(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createProductJson(String token, Long categoryId, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"desc\",\"price\":\"" + price + "\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createOrder(String subdomain, String body) throws Exception {
        MvcResult result = mockMvc.perform(withSubdomain(post("/api/loja/pedidos"), subdomain)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions advance(String token, long orderId, String fromStatus) throws Exception {
        return mockMvc.perform(post("/api/painel/orders/" + orderId + "/advance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromStatus\":\"" + fromStatus + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions cancel(String token, long orderId, String reason) throws Exception {
        return mockMvc.perform(post("/api/painel/orders/" + orderId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("reason", reason))));
    }

    private MockHttpServletRequestBuilder withSubdomain(MockHttpServletRequestBuilder builder, String subdomain) {
        return builder.with(request -> {
            request.setServerName(TestDomain.host(subdomain));
            return request;
        });
    }

    private String orderJson(String idempotencyKey, Long productId, int quantity, List<Long> additionalItemIds) {
        return """
                {"idempotencyKey":"%s","customerName":"Cliente Teste","customerPhone":"85988887777",
                 "deliveryType":"RETIRADA","notes":"","lines":[{"productId":%d,"quantity":%d,"additionalItemIds":%s}]}
                """.formatted(idempotencyKey, productId, quantity, additionalItemIds);
    }

    private int count(String table, String where) {
        Integer value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Integer.class);
        return value == null ? 0 : value;
    }

    private int tenantOrderCount(Long tenantId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT order_count_month FROM tenants WHERE id = ?", Integer.class, tenantId);
        return value == null ? 0 : value;
    }
}
