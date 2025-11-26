package com.exercise.atm.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    private static final String CARD = "4111111111111111";
    private static final String PIN = "p@ssw0rd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("cardNumber", CARD, "pin", PIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.customerId").isNotEmpty())
                .andExpect(jsonPath("$.customerName").value("Alice Carter"));
    }

    @Test
    void loginWithInvalidPinReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("cardNumber", CARD, "pin", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void depositUpdatesBalance() throws Exception {
        String token = loginAndGetToken();

        MvcResult result = mockMvc.perform(
                        post("/api/v1/account/deposit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("200.00")))))
                .andExpect(status().isOk())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(new BigDecimal(node.get("balance").asText())).isEqualByComparingTo("1400.00");
        assertThat(new BigDecimal(node.get("withdrawnToday").asText())).isEqualByComparingTo("0");
        assertThat(new BigDecimal(node.get("dailyLimit").asText())).isEqualByComparingTo("500.00");
        assertThat(node.get("customerName").asText()).isEqualTo("Alice Carter");
    }

    @Test
    void withdrawWithInsufficientFundsReturnsConflict() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(
                        post("/api/v1/account/withdraw")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("2000.00")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void withdrawExceedingDailyLimitReturnsConflict() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(
                        post("/api/v1/account/withdraw")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("450.00")))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/account/withdraw")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("100.00")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Daily withdrawal limit exceeded"));
    }

    @Test
    void withdrawWithinLimitsSucceeds() throws Exception {
        String token = loginAndGetToken();

        MvcResult result = mockMvc.perform(
                        post("/api/v1/account/withdraw")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("100.00")))))
                .andExpect(status().isOk())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(new BigDecimal(node.get("balance").asText())).isEqualByComparingTo("1100.00");
        assertThat(new BigDecimal(node.get("withdrawnToday").asText())).isEqualByComparingTo("100.00");
        assertThat(new BigDecimal(node.get("dailyLimit").asText())).isEqualByComparingTo("500.00");
        assertThat(node.get("customerId").asText()).isNotBlank();
        assertThat(node.get("customerName").asText()).isNotBlank();
    }

    @Test
    void snapshotReturnsCurrentState() throws Exception {
        String token = loginAndGetToken();

        MvcResult result = mockMvc.perform(get("/api/v1/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(node.get("customerName").asText()).isNotBlank();
        assertThat(node.get("customerName").asText()).isEqualTo("Alice Carter");
        assertThat(new BigDecimal(node.get("balance").asText())).isEqualByComparingTo("1200.00");
        assertThat(new BigDecimal(node.get("dailyLimit").asText())).isEqualByComparingTo("500.00");
        assertThat(new BigDecimal(node.get("withdrawnToday").asText())).isEqualByComparingTo("0");
    }

    @Test
    void genericExceptionYieldsInternalError() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(
                        post("/api/v1/account/deposit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\": \"not-a-number\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("cardNumber", CARD, "pin", PIN))))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Map<?, ?> payload = objectMapper.readValue(body, Map.class);
        String token = (String) payload.get("accessToken");
        assertThat(token).isNotBlank();
        return token;
    }
}
