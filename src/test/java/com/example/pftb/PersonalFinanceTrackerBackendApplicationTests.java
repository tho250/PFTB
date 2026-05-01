package com.example.pftb;

import com.example.pftb.dto.AuthRequest;
import com.example.pftb.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonalFinanceTrackerBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void v1TransactionFlowStillWorks() throws Exception {
        String token = registerAndLogin("v1user", "v1user@example.com");

        String createBody = """
                {
                  "title":"Salary",
                  "amount":1000,
                  "type":"INCOME",
                  "category":"Work",
                  "date":"2026-04-20"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Salary"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Work"));
    }

    @Test
    void messageRulesCrudAndIsolation() throws Exception {
        String user1Token = registerAndLogin("rules1", "rules1@example.com");
        String user2Token = registerAndLogin("rules2", "rules2@example.com");

        String payload = """
                {
                  "name":"Incoming transfer",
                  "enabled":true,
                  "directionType":"INCOME",
                  "keywords":["received","from"],
                  "category":"Parsed Message"
                }
                """;

        String response = mockMvc.perform(post("/api/message-rules")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Incoming transfer"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/message-rules")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/api/message-rules/{id}", id)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isBadRequest());

        String update = """
                {
                  "name":"Incoming transfer updated",
                  "enabled":true,
                  "directionType":"INCOME",
                  "keywords":["received"],
                  "category":"Income"
                }
                """;

        mockMvc.perform(put("/api/message-rules/{id}", id)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Incoming transfer updated"));
    }

    @Test
    void parsePreviewWorksForBasicCase() throws Exception {
        String token = registerAndLogin("parse1", "parse1@example.com");
        String ruleBody = """
                {
                  "name":"Received money",
                  "enabled":true,
                  "directionType":"INCOME",
                  "keywords":["received","from"]
                }
                """;

        mockMvc.perform(post("/api/message-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleBody))
                .andExpect(status().isOk());

        String previewBody = """
                {
                  "rawText":"You have received 5000 RWF from Alice",
                  "sourceType":"SMS"
                }
                """;

        mockMvc.perform(post("/api/message-events/parse-preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.detectedType").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(5000));
    }

    @Test
    void messageEventsAreUserIsolatedAndConvertible() throws Exception {
        String user1Token = registerAndLogin("event1", "event1@example.com");
        String user2Token = registerAndLogin("event2", "event2@example.com");

        String eventBody = """
                {
                  "rawText":"Payment of 12000 RWF sent to MTN",
                  "sourceType":"MANUAL",
                  "matched":true,
                  "detectedType":"PAYMENT_SENT",
                  "amount":12000,
                  "currency":"RWF",
                  "counterparty":"MTN",
                  "eventDate":"2026-04-27",
                  "reviewStatus":"PENDING"
                }
                """;

        String created = mockMvc.perform(post("/api/message-events")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/message-events")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/message-events/{id}/convert-to-transaction", id)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/message-events/{id}/convert-to-transaction", id)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void paymentObligationsCrudRecordPaymentAndSummary() throws Exception {
        String token = registerAndLogin("ob1", "ob1@example.com");
        LocalDate dueDate = LocalDate.now().minusDays(1);

        String createBody = """
                {
                  "title":"Supplier invoice",
                  "counterparty":"Supplier X",
                  "type":"PAYABLE",
                  "amount":45000,
                  "dueDate":"%s",
                  "priority":"HIGH",
                  "notes":"April",
                  "reminderEnabled":true
                }
                """.formatted(dueDate);

        String created = mockMvc.perform(post("/api/payment-obligations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OVERDUE"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        String paymentBody = """
                {
                  "amount":10000,
                  "date":"2026-04-27",
                  "note":"Partial payment received",
                  "createTransaction":true
                }
                """;

        mockMvc.perform(post("/api/payment-obligations/{id}/record-payment", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").value(10000))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

        mockMvc.perform(get("/api/payment-obligations/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueCount").value(0))
                .andExpect(jsonPath("$.highPriorityCount").value(1));
    }

    @Test
    void insightsSummaryReturnsExpectedFields() throws Exception {
        String token = registerAndLogin("ins1", "ins1@example.com");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Salary",
                                  "amount":200000,
                                  "type":"INCOME",
                                  "category":"Work",
                                  "date":"2026-04-10"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Groceries",
                                  "amount":50000,
                                  "type":"EXPENSE",
                                  "category":"Food",
                                  "date":"2026-04-11"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/insights/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMonthIncome").exists())
                .andExpect(jsonPath("$.currentMonthExpense").exists())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.disclaimer").exists());
    }

    private String registerAndLogin(String username, String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername(username);
        authRequest.setPassword("Password@123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(loginResponse);
        assertThat(node.get("token").asText()).isNotBlank();
        return node.get("token").asText();
    }
}
