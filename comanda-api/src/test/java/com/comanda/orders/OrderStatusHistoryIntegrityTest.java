package com.comanda.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.comanda.orders.domain.OrderStatusHistory;
import com.comanda.orders.domain.OrderStatusHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
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
 * Covers task 7.7 (PRD 4.4): {@code order_status_history} records every field correctly and is
 * append-only. Immutability is enforced in code, not by a DB grant/trigger (design.md Risks —
 * that hardening is out of MVP scope): {@link OrderStatusHistory} declares no setters at all, and
 * {@link OrderStatusHistoryRepository} declares no update/delete query method — both asserted here
 * by reflection, on top of the end-to-end field check.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderStatusHistoryIntegrityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void historyEntityIsImmutableByConstruction() {
        boolean hasSetter = Stream.of(OrderStatusHistory.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.startsWith("set"));
        assertThat(hasSetter).as("OrderStatusHistory must declare no setters").isFalse();
    }

    @Test
    void repositoryDeclaresNoUpdateOrDeleteQueryMethod() {
        boolean hasMutatingMethod = Stream.of(OrderStatusHistoryRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .map(String::toLowerCase)
                .anyMatch(name -> name.contains("delete") || name.contains("update") || name.contains("remove"));
        assertThat(hasMutatingMethod)
                .as("OrderStatusHistoryRepository must declare no custom update/delete method")
                .isFalse();
    }

    @Test
    void eachTransitionAppendsACompleteRecordAndNothingIsMutated() throws Exception {
        String token = signupAndGetToken("history-tenant", "history@example.com");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'history@example.com'", Long.class);
        Long categoryId = createCategory(token, "Marmitas").get("id").asLong();
        Long productId = createProduct(token, categoryId, "Marmita", "18.00").get("id").asLong();
        long orderId = createOrder(token, productId).get("id").asLong();

        advance(token, orderId, "RECEBIDO").andExpect(status().isOk());
        advance(token, orderId, "ACEITO").andExpect(status().isOk());

        List<OrderStatusHistoryRow> rows = jdbcTemplate.query(
                "SELECT from_status, to_status, changed_by_user_id, created_at FROM order_status_history "
                        + "WHERE order_id = ? ORDER BY created_at ASC, id ASC",
                (rs, rowNum) -> new OrderStatusHistoryRow(
                        rs.getString("from_status"), rs.getString("to_status"), (Long) rs.getObject("changed_by_user_id"),
                        rs.getObject("created_at")),
                orderId);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).fromStatus()).isNull();
        assertThat(rows.get(0).toStatus()).isEqualTo("RECEBIDO");
        assertThat(rows.get(0).changedByUserId()).isNull(); // SYSTEM, on creation

        assertThat(rows.get(1).fromStatus()).isEqualTo("RECEBIDO");
        assertThat(rows.get(1).toStatus()).isEqualTo("ACEITO");
        assertThat(rows.get(1).changedByUserId()).isEqualTo(userId);

        assertThat(rows.get(2).fromStatus()).isEqualTo("ACEITO");
        assertThat(rows.get(2).toStatus()).isEqualTo("EM_PREPARO");
        assertThat(rows.get(2).changedByUserId()).isEqualTo(userId);

        rows.forEach(r -> assertThat(r.createdAt()).isNotNull());

        // A later, unrelated transition on the same order must not touch earlier rows' timestamps.
        Object firstRowCreatedAtBefore = rows.get(0).createdAt();
        advance(token, orderId, "EM_PREPARO").andExpect(status().isOk());
        Object firstRowCreatedAtAfter = jdbcTemplate.queryForObject(
                "SELECT created_at FROM order_status_history WHERE order_id = ? ORDER BY created_at ASC, id ASC LIMIT 1",
                Object.class,
                orderId);
        assertThat(firstRowCreatedAtAfter).isEqualTo(firstRowCreatedAtBefore);
        assertThat(count(orderId)).isEqualTo(4);
    }

    private record OrderStatusHistoryRow(String fromStatus, String toStatus, Long changedByUserId, Object createdAt) {
    }

    private int count(long orderId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_status_history WHERE order_id = ?", Integer.class, orderId);
        return value == null ? 0 : value;
    }

    private org.springframework.test.web.servlet.ResultActions advance(String token, long orderId, String fromStatus)
            throws Exception {
        return mockMvc.perform(post("/api/painel/orders/" + orderId + "/advance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromStatus\":\"" + fromStatus + "\"}"));
    }

    private String signupAndGetToken(String subdomain, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dono","businessName":"Negocio","subdomain":"%s","whatsappNumber":"85999990000","email":"%s","password":"senha123"}
                                """.formatted(subdomain, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
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

    private JsonNode createProduct(String token, Long categoryId, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"\",\"price\":\"" + price + "\",\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createOrder(String token, Long productId) throws Exception {
        MvcResult signupUser = null;
        String subdomain = jdbcTemplate.queryForObject(
                "SELECT t.subdomain FROM tenants t JOIN users u ON u.tenant_id = t.id WHERE u.email = 'history@example.com'",
                String.class);
        MvcResult result = mockMvc.perform(post("/api/loja/pedidos")
                        .with(request -> {
                            request.setServerName(subdomain + ".comanda.local");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s","customerName":"Cliente Teste","customerPhone":"85988887777",
                                 "deliveryType":"RETIRADA","notes":"","lines":[{"productId":%d,"quantity":1,"additionalItemIds":[]}]}
                                """.formatted(UUID.randomUUID(), productId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
