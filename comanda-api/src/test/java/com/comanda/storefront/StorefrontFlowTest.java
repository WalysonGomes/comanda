package com.comanda.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
 * Covers tasks 5.1-5.4: cross-tenant isolation on the public routes (cardápio + cart validation),
 * day-of-week/manual-toggle availability, business-hours-driven open/closed, and price/availability
 * reconciliation at checkout. Signs up real tenants through {@code owner-auth} and builds their
 * menu through the real {@code menu-management} endpoints — exactly like
 * {@code MenuManagementFlowTest} — then hits {@code /api/loja/**} with the subdomain as the
 * request's {@code Host}, the same way a real customer's browser would.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class StorefrontFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int TODAY = BusinessClock.toPrdDayOfWeek(LocalDate.now(BusinessClock.ZONE).getDayOfWeek());
    private static final int NOT_TODAY = (TODAY + 3) % 7;

    // ---------- 5.1 isolamento + 5.2 disponibilidade ----------

    @Test
    void publicMenuOnlyShowsAvailableProductsOfItsOwnTenant() throws Exception {
        String subdomainA = "loja-a-menu";
        String tokenA = signupAndGetToken(subdomainA, "loja-a-menu@example.com");
        String tokenB = signupAndGetToken("loja-b-menu", "loja-b-menu@example.com");

        Long categoryId = createCategory(tokenA, "Lanches").get("id").asLong();
        createProduct(tokenA, categoryId, "X-Salada", "18.00", null, null).get("id").asLong();
        createProduct(tokenA, categoryId, "Fora do dia", "12.00", new int[] {NOT_TODAY}, null);
        createProduct(tokenA, categoryId, "Indisponível", "9.00", null, false);

        Long categoryOfB = createCategory(tokenB, "Sucos").get("id").asLong();
        createProduct(tokenB, categoryOfB, "Suco de B", "7.00", null, null);

        MvcResult result = mockMvc.perform(loja(get("/api/loja/cardapio"), subdomainA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(categories).hasSize(1);
        JsonNode products = categories.get(0).get("products");
        assertThat(products).hasSize(1);
        assertThat(products.get(0).get("name").asText()).isEqualTo("X-Salada");
    }

    @Test
    void forgedCrossTenantProductIdIsInaccessibleOnPublicRoutes() throws Exception {
        String tokenA = signupAndGetToken("loja-a-idor", "loja-a-idor@example.com");
        String subdomainB = "loja-b-idor";
        signupAndGetToken(subdomainB, "loja-b-idor@example.com");

        Long categoryOfA = createCategory(tokenA, "Combos").get("id").asLong();
        Long productOfA = createProduct(tokenA, categoryOfA, "Combo A", "30.00", null, null).get("id").asLong();

        String body = """
                {"lines":[{"lineId":"l1","productId":%d,"quantity":1,"additionalItemIds":[]}],"deliveryType":"RETIRADA"}
                """.formatted(productOfA);

        mockMvc.perform(loja(post("/api/loja/carrinho/validar"), subdomainB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.lines[0].available").value(false));
    }

    // ---------- 5.3 aberto/fechado ----------

    @Test
    void openBusinessHoursWindowMarksTheStoreAsOpen() throws Exception {
        String subdomain = "loja-aberta";
        signupAndGetToken(subdomain, "loja-aberta@example.com");
        Long tenantId = tenantIdBySubdomain(subdomain);
        insertBusinessHours(tenantId, TODAY, "00:00:01", "23:59:59", false);

        mockMvc.perform(loja(get("/api/loja/negocio"), subdomain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.reopensLabel").doesNotExist());
    }

    @Test
    void dayMarkedClosedInBusinessHoursOverridesTheWindow() throws Exception {
        String subdomain = "loja-fechada-dia";
        signupAndGetToken(subdomain, "loja-fechada-dia@example.com");
        Long tenantId = tenantIdBySubdomain(subdomain);
        insertBusinessHours(tenantId, TODAY, "00:00:01", "23:59:59", true);

        mockMvc.perform(loja(get("/api/loja/negocio"), subdomain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.reopensLabel").exists());
    }

    @Test
    void manualToggleClosesTheStoreEvenInsideTheScheduledWindow() throws Exception {
        String subdomain = "loja-toggle-manual";
        signupAndGetToken(subdomain, "loja-toggle-manual@example.com");
        Long tenantId = tenantIdBySubdomain(subdomain);
        insertBusinessHours(tenantId, TODAY, "00:00:01", "23:59:59", false);
        jdbcTemplate.update("UPDATE tenants SET is_open = false WHERE id = ?", tenantId);

        mockMvc.perform(loja(get("/api/loja/negocio"), subdomain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.reopensLabel").value("Fechado pelo estabelecimento no momento."));
    }

    // ---------- 5.4 validação de finalização ----------

    @Test
    void validCartIsReconciledWithAuthoritativeServerPrices() throws Exception {
        String subdomain = "loja-cart-valido";
        String token = signupAndGetToken(subdomain, "loja-cart-valido@example.com");
        Long categoryId = createCategory(token, "Pratos").get("id").asLong();
        Long productId = createProduct(token, categoryId, "Marmita", "15.00", null, null).get("id").asLong();
        Long groupId = createGroup(token, productId, "Adicionais", "MULTIPLE", 0, 2).get("id").asLong();
        Long itemId = createItem(token, groupId, "Ovo frito", "2.00").get("id").asLong();
        setDeliveryFee(tenantIdBySubdomain(subdomain), "6.00");

        String body = """
                {"lines":[{"lineId":"l1","productId":%d,"quantity":2,"additionalItemIds":[%d]}],"deliveryType":"ENTREGA"}
                """.formatted(productId, itemId);

        mockMvc.perform(loja(post("/api/loja/carrinho/validar"), subdomain)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(17.00))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(34.00))
                .andExpect(jsonPath("$.subtotal").value(34.00))
                .andExpect(jsonPath("$.deliveryFee").value(6.00))
                .andExpect(jsonPath("$.total").value(40.00));
    }

    @Test
    void itemThatBecameUnavailableMidSessionIsFlaggedAtCheckout() throws Exception {
        String subdomain = "loja-cart-esgotado";
        String token = signupAndGetToken(subdomain, "loja-cart-esgotado@example.com");
        Long categoryId = createCategory(token, "Pratos").get("id").asLong();
        Long productId = createProduct(token, categoryId, "Marmita", "15.00", null, null).get("id").asLong();

        mockMvc.perform(patch("/api/painel/products/" + productId + "/availability")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().is2xxSuccessful());

        String body = """
                {"lines":[{"lineId":"l1","productId":%d,"quantity":1,"additionalItemIds":[]}],"deliveryType":"RETIRADA"}
                """.formatted(productId);

        mockMvc.perform(loja(post("/api/loja/carrinho/validar"), subdomain)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.lines[0].available").value(false))
                .andExpect(jsonPath("$.lines[0].unitPrice").doesNotExist());
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder loja(MockHttpServletRequestBuilder builder, String subdomain) {
        return builder.with(request -> {
            request.setServerName(subdomain + ".comanda.local");
            return request;
        });
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

    private JsonNode createProduct(
            String token, Long categoryId, String name, String price, int[] availableDays, Boolean available)
            throws Exception {
        String daysJson = availableDays == null
                ? "null"
                : "[" + String.join(",", java.util.Arrays.stream(availableDays).mapToObj(String::valueOf).toList()) + "]";
        String availableJson = available == null ? "null" : available.toString();
        MvcResult result = mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"desc","price":"%s","categoryId":%d,"availableDays":%s,"available":%s}
                                """.formatted(name, price, categoryId, daysJson, availableJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createGroup(String token, Long productId, String name, String selectionType, int min, Integer max)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/products/" + productId + "/additional-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","required":true,"selectionType":"%s","minSelections":%d,"maxSelections":%s}
                                """.formatted(name, selectionType, min, max == null ? "null" : max)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createItem(String token, Long groupId, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/additional-groups/" + groupId + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"additionalPrice\":\"" + price + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Long tenantIdBySubdomain(String subdomain) {
        return jdbcTemplate.queryForObject("SELECT id FROM tenants WHERE subdomain = ?", Long.class, subdomain);
    }

    private void insertBusinessHours(Long tenantId, int dayOfWeek, String opensAt, String closesAt, boolean closed) {
        jdbcTemplate.update(
                "INSERT INTO business_hours (tenant_id, day_of_week, opens_at, closes_at, is_closed) VALUES (?, ?, ?, ?, ?)",
                tenantId, dayOfWeek, java.sql.Time.valueOf(opensAt), java.sql.Time.valueOf(closesAt), closed);
    }

    private void setDeliveryFee(Long tenantId, String fee) {
        jdbcTemplate.update("UPDATE tenants SET delivery_fee = ? WHERE id = ?", new java.math.BigDecimal(fee), tenantId);
    }
}
