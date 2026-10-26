package com.yassir.jobflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String email =
                "duplicate-" + UUID.randomUUID() + "@example.com";

        String requestBody =
                "{\"firstName\":\"Test\","
                + "\"lastName\":\"User\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"ValidPassword123!\"}";

        // First registration should succeed
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"));

        // Registering with the same email should return 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Email is already in use"));
    }
}