package com.comanda.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers tasks 6.1-6.4: cross-tenant isolation (IDOR) on every resource in the hierarchy,
 * validation of name/price/min-max coherence, magic-byte upload validation with the
 * upload-never-blocks-save guarantee (PRD 4.5), and the category/product removal rules
 * (design.md Decision 6). Signs up two real tenants through {@code owner-auth} to get real JWTs,
 * exactly like {@code OwnerAuthFlowTest} does.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MenuManagementFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5};
    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5, 6};
    private static final byte[] NOT_AN_IMAGE_BYTES = "isso nao e uma imagem de verdade".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- 6.1 isolamento / IDOR ----------

    @Test
    void categoryOfAnotherTenantIsInaccessible() throws Exception {
        String tokenA = signupAndGetToken("cat-tenant-a", "cat-a@example.com");
        String tokenB = signupAndGetToken("cat-tenant-b", "cat-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Lanches").get("id").asLong();

        mockMvc.perform(put("/api/painel/categories/" + categoryIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hackeado\",\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        mockMvc.perform(delete("/api/painel/categories/" + categoryIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void productOfAnotherTenantIsInaccessible() throws Exception {
        String tokenA = signupAndGetToken("prod-tenant-a", "prod-a@example.com");
        String tokenB = signupAndGetToken("prod-tenant-b", "prod-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Bebidas").get("id").asLong();
        Long productIdOfA = createProduct(tokenA, categoryIdOfA, "Suco", "10.00").get("id").asLong();

        mockMvc.perform(put("/api/painel/products/" + productIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryIdOfA, "Hackeado", "1.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void productCannotBeLinkedToCategoryOfAnotherTenant() throws Exception {
        String tokenA = signupAndGetToken("link-tenant-a", "link-a@example.com");
        String tokenB = signupAndGetToken("link-tenant-b", "link-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Sobremesas").get("id").asLong();

        mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryIdOfA, "Pudim", "8.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void additionalGroupAndItemOfAnotherTenantAreInaccessible() throws Exception {
        String tokenA = signupAndGetToken("add-tenant-a", "add-a@example.com");
        String tokenB = signupAndGetToken("add-tenant-b", "add-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Combos").get("id").asLong();
        Long productIdOfA = createProduct(tokenA, categoryIdOfA, "Combo 1", "20.00").get("id").asLong();
        Long groupIdOfA = createGroup(tokenA, productIdOfA, "Tamanho", "SINGLE", 1, 1).get("id").asLong();
        Long itemIdOfA = createItem(tokenA, groupIdOfA, "Grande", "3.00").get("id").asLong();

        mockMvc.perform(get("/api/painel/products/" + productIdOfA + "/additional-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(put("/api/painel/additional-groups/" + groupIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupJson("Hackeado", "SINGLE", 0, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDITIONAL_GROUP_NOT_FOUND"));

        mockMvc.perform(put("/api/painel/additional-items/" + itemIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hackeado\",\"additionalPrice\":\"0.00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDITIONAL_ITEM_NOT_FOUND"));
    }

    @Test
    void productAvailabilityAndDeleteOfAnotherTenantAreInaccessible() throws Exception {
        String tokenA = signupAndGetToken("avail-tenant-a", "avail-a@example.com");
        String tokenB = signupAndGetToken("avail-tenant-b", "avail-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Pratos").get("id").asLong();
        Long productIdOfA = createProduct(tokenA, categoryIdOfA, "Feijoada", "22.00").get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/painel/products/" + productIdOfA + "/availability")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(delete("/api/painel/products/" + productIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void additionalItemCreationAndDeletionOnAnotherTenantAreInaccessible() throws Exception {
        String tokenA = signupAndGetToken("add2-tenant-a", "add2-a@example.com");
        String tokenB = signupAndGetToken("add2-tenant-b", "add2-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Sorvetes").get("id").asLong();
        Long productIdOfA = createProduct(tokenA, categoryIdOfA, "Sundae", "14.00").get("id").asLong();
        Long groupIdOfA = createGroup(tokenA, productIdOfA, "Cobertura", "SINGLE", 1, 1).get("id").asLong();
        Long itemIdOfA = createItem(tokenA, groupIdOfA, "Chocolate", "2.00").get("id").asLong();

        mockMvc.perform(post("/api/painel/additional-groups/" + groupIdOfA + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hackeado\",\"additionalPrice\":\"0.00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDITIONAL_GROUP_NOT_FOUND"));

        mockMvc.perform(delete("/api/painel/additional-groups/" + groupIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDITIONAL_GROUP_NOT_FOUND"));

        mockMvc.perform(delete("/api/painel/additional-items/" + itemIdOfA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDITIONAL_ITEM_NOT_FOUND"));
    }

    @Test
    void reorderWithAnotherTenantsIdsIsRejected() throws Exception {
        String tokenA = signupAndGetToken("reorder-tenant-a", "reorder-a@example.com");
        String tokenB = signupAndGetToken("reorder-tenant-b", "reorder-b@example.com");

        Long categoryIdOfA = createCategory(tokenA, "Bebidas").get("id").asLong();
        Long productIdOfA = createProduct(tokenA, categoryIdOfA, "Refrigerante", "7.00").get("id").asLong();
        Long categoryIdOfB = createCategory(tokenB, "Lanches").get("id").asLong();

        mockMvc.perform(post("/api/painel/categories/reorder")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\":[" + categoryIdOfA + "]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        mockMvc.perform(post("/api/painel/products/reorder")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + categoryIdOfB + ",\"orderedIds\":[" + productIdOfA + "]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    // ---------- 6.2 validação ----------

    @Test
    void blankCategoryNameIsRejected() throws Exception {
        String token = signupAndGetToken("valid-cat-tenant", "valid-cat@example.com");

        mockMvc.perform(post("/api/painel/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void negativeProductPriceIsRejected() throws Exception {
        String token = signupAndGetToken("valid-price-tenant", "valid-price@example.com");
        Long categoryId = createCategory(token, "Pratos").get("id").asLong();

        mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "Prato executivo", "-5.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void incoherentSelectionLimitsAreRejected() throws Exception {
        String token = signupAndGetToken("valid-group-tenant", "valid-group@example.com");
        Long categoryId = createCategory(token, "Pizzas").get("id").asLong();
        Long productId = createProduct(token, categoryId, "Pizza grande", "45.00").get("id").asLong();

        mockMvc.perform(post("/api/painel/products/" + productId + "/additional-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupJson("Bordas", "MULTIPLE", 3, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ---------- 6.3 upload ----------

    @Test
    void uploadWithValidMagicBytesSucceeds() throws Exception {
        String token = signupAndGetToken("upload-ok-tenant", "upload-ok@example.com");
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG_BYTES);

        mockMvc.perform(multipart("/api/painel/images").file(file).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/media/")));

        MockMultipartFile pngFile = new MockMultipartFile("file", "foto.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
        mockMvc.perform(multipart("/api/painel/images").file(pngFile).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void fakeExtensionWithWrongMagicBytesIsRejected() throws Exception {
        String token = signupAndGetToken("upload-fake-tenant", "upload-fake@example.com");
        MockMultipartFile file =
                new MockMultipartFile("file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, NOT_AN_IMAGE_BYTES);

        mockMvc.perform(multipart("/api/painel/images").file(file).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));
    }

    @Test
    void productIsSavedWithoutPhotoWhenUploadFails() throws Exception {
        String token = signupAndGetToken("upload-fallback-tenant", "upload-fallback@example.com");
        MockMultipartFile badFile =
                new MockMultipartFile("file", "foto.png", MediaType.IMAGE_PNG_VALUE, NOT_AN_IMAGE_BYTES);

        mockMvc.perform(multipart("/api/painel/images").file(badFile).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());

        Long categoryId = createCategory(token, "Marmitas").get("id").asLong();
        mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "Marmita média", "15.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    // ---------- 6.4 remoção ----------

    @Test
    void removingNonEmptyCategoryIsBlocked() throws Exception {
        String token = signupAndGetToken("nonempty-cat-tenant", "nonempty-cat@example.com");
        Long categoryId = createCategory(token, "Salgados").get("id").asLong();
        createProduct(token, categoryId, "Coxinha", "6.00");

        mockMvc.perform(delete("/api/painel/categories/" + categoryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_EMPTY"));
    }

    @Test
    void removingProductCascadesAdditionalGroupsAndItems() throws Exception {
        String token = signupAndGetToken("cascade-tenant", "cascade@example.com");
        Long categoryId = createCategory(token, "Burgers").get("id").asLong();
        Long productId = createProduct(token, categoryId, "X-Tudo", "25.00").get("id").asLong();
        Long groupId = createGroup(token, productId, "Ponto da carne", "SINGLE", 1, 1).get("id").asLong();
        createItem(token, groupId, "Mal passado", "0.00");

        assertThat(count("additional_groups", "product_id = " + productId)).isEqualTo(1);
        assertThat(count("additional_items", "additional_group_id = " + groupId)).isEqualTo(1);

        mockMvc.perform(delete("/api/painel/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(count("additional_groups", "product_id = " + productId)).isZero();
        assertThat(count("additional_items", "additional_group_id = " + groupId)).isZero();
        assertThat(count("products", "id = " + productId)).isZero();
    }

    // ---------- helpers ----------

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

    private com.fasterxml.jackson.databind.JsonNode createCategory(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private com.fasterxml.jackson.databind.JsonNode createProduct(
            String token, Long categoryId, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, name, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private com.fasterxml.jackson.databind.JsonNode createGroup(
            String token, Long productId, String name, String selectionType, int min, Integer max) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/products/" + productId + "/additional-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupJson(name, selectionType, min, max)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private com.fasterxml.jackson.databind.JsonNode createItem(String token, Long groupId, String name, String price)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/painel/additional-groups/" + groupId + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"additionalPrice\":\"" + price + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String productJson(Long categoryId, String name, String price) {
        return """
                {"name":"%s","description":"desc","price":"%s","categoryId":%d}
                """.formatted(name, price, categoryId);
    }

    private String groupJson(String name, String selectionType, int min, Integer max) {
        return """
                {"name":"%s","required":true,"selectionType":"%s","minSelections":%d,"maxSelections":%s}
                """.formatted(name, selectionType, min, max == null ? "null" : max);
    }

    private int count(String table, String where) {
        Integer value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Integer.class);
        return value == null ? 0 : value;
    }
}
