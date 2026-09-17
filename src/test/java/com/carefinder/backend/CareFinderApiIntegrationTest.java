package com.carefinder.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareFinderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicHospitalSearchReturnsFrontendCompatibleHospitalCards() throws Exception {
        mockMvc.perform(get("/api/v1/hospitals")
                        .queryParam("location", "Delhi")
                        .queryParam("emergency", "true")
                        .queryParam("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].name").isString())
                .andExpect(jsonPath("$.content[0].insurance").isArray())
                .andExpect(jsonPath("$.content[0].specialties").isArray())
                .andExpect(jsonPath("$.content[0].emergency").value(true));
    }

    @Test
    void userCanRegisterLoginAndSaveFavorite() throws Exception {
        String registration = """
                {
                  "name": "Asha User",
                  "email": "asha@example.com",
                  "mobile": "+91 98765 43210",
                  "password": "StrongPass123",
                  "city": "New Delhi",
                  "insuranceProvider": "Star Health"
                }
                """;

        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        String accessToken = response.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(post("/api/v1/me/favorites/1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospital.id").value(1));

        mockMvc.perform(get("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hospital.id").value(1));
    }

    @Test
    void comparisonRequiresTwoOrThreeDifferentHospitals() throws Exception {
        mockMvc.perform(get("/api/v1/hospitals/compare").queryParam("ids", "1", "2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitals.length()").value(3))
                .andExpect(jsonPath("$.highlights.highestRatedHospitalId").isNumber());

        mockMvc.perform(get("/api/v1/hospitals/compare").queryParam("ids", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatbotReturnsHospitalCardsWithoutPaidApi() throws Exception {
        mockMvc.perform(post("/api/v1/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Show emergency hospitals in Delhi","language":"en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("EMERGENCY_SEARCH"))
                .andExpect(jsonPath("$.hospitals").isArray())
                .andExpect(jsonPath("$.emergencyDisclaimer").value(true));
    }
}
