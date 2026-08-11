package com.adwitiya.feedbackportal.integration;

import com.adwitiya.feedbackportal.web.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication API")
class AuthApiIT extends AbstractIntegrationTest {
    private static final String STUDENT_EMAIL = "aarav.sharma1@student.university.edu";
    private static final String ADMIN_EMAIL = "priya.menon@university.edu";
    private static final String PASSWORD = "Password#123";

    @Test
    @DisplayName("valid credentials return a token pair and the caller's profile")
    void loginSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest(ADMIN_EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))

                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("a wrong password and an unknown account are indistinguishable")
    void failuresAreIndistinguishable() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest(ADMIN_EMAIL, "WrongPassword#1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest("ghost@university.edu", "WrongPassword#1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    @DisplayName("malformed credentials are rejected by bean validation")
    void validationRejectsMalformedInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest("not-an-email", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("protected endpoints reject an anonymous caller with problem+json")
    void anonymousAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthenticated"));
    }

    @Test
    @DisplayName("a garbage bearer token does not authenticate")
    void garbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/feedback").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a refresh token is single-use")
    void refreshTokenRotates() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest(STUDENT_EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the health endpoint is public")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
