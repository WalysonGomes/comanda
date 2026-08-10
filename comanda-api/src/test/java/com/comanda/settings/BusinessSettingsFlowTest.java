package com.comanda.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comanda.ComandaApiApplication;
import com.comanda.TestDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = ComandaApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class BusinessSettingsFlowTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void ownerReadsAndUpdatesAllFieldsAndStorefrontReflectsThem() throws Exception {
        String token = signup("settings-a");
        mvc.perform(get("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.businessName").value("Loja settings-a"));
        mvc.perform(put("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(settings("Loja Atualizada", "85999998888", "12.50", "30.00", "settings-new")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subdomain").value("settings-new"))
                .andExpect(jsonPath("$.businessHours.length()").value(7));
        mvc.perform(get("/api/loja/negocio").with(request -> { request.setServerName(TestDomain.host("settings-new")); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Loja Atualizada"))
                .andExpect(jsonPath("$.deliveryFee").value(12.50));
        mvc.perform(get("/api/loja/negocio").with(request -> { request.setServerName(TestDomain.host("settings-a")); return request; }))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservedAndDuplicateSubdomainsAreRejectedWithoutPartialUpdates() throws Exception {
        String tokenA = signup("settings-b"); signup("settings-taken");
        mvc.perform(put("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content(settings("Should Not Persist", "85999998888", "1", "2", " API ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content(settings("Should Not Persist", "85999998888", "1", "2", "settings-taken")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SUBDOMAIN_ALREADY_IN_USE"));
        mvc.perform(get("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(jsonPath("$.businessName").value("Loja settings-b"));
    }

    @Test
    void invalidMoneyWhatsappAndHoursAreAtomic() throws Exception {
        String token = signup("settings-c");
        mvc.perform(put("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(settings("Changed", "bad", "-1", "0", "settings-c")))
                .andExpect(status().isBadRequest());
        String invalidHours = settings("Changed", "85999998888", "1", "2", "settings-c").replace("\"closesAt\":\"18:00\"", "\"closesAt\":\"07:00\"");
        mvc.perform(put("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(invalidHours)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/painel/business-settings").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.businessName").value("Loja settings-c"));
    }

    @Test
    void logoUsesMagicBytesAndIsTenantScoped() throws Exception {
        String token = signup("settings-logo");
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0};
        mvc.perform(multipart("/api/painel/business-settings/logo").file(new MockMultipartFile("file", "logo.jpg", "image/jpeg", png))
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.containsString("/media/")));
        mvc.perform(multipart("/api/painel/business-settings/logo").file(new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes()))
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));
    }

    private String signup(String slug) throws Exception {
        String email = slug + "-" + UUID.randomUUID() + "@example.test";
        String body = "{\"name\":\"Owner\",\"businessName\":\"Loja " + slug + "\",\"subdomain\":\"" + slug + "\",\"whatsappNumber\":\"85999990000\",\"email\":\"" + email + "\",\"password\":\"senha123\"}";
        String response = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }
    private String bearer(String token) { return "Bearer " + token; }
    private String settings(String name, String whatsapp, String fee, String minimum, String slug) {
        StringBuilder hours = new StringBuilder();
        for (int day = 0; day < 7; day++) { if (day > 0) hours.append(','); hours.append("{\"dayOfWeek\":").append(day).append(",\"opensAt\":\"08:00\",\"closesAt\":\"18:00\",\"closed\":false}"); }
        return "{\"businessName\":\"" + name + "\",\"whatsappNumber\":\"" + whatsapp + "\",\"deliveryFee\":" + fee + ",\"minOrderValue\":" + minimum + ",\"subdomain\":\"" + slug + "\",\"businessHours\":[" + hours + "]}";
    }
}
