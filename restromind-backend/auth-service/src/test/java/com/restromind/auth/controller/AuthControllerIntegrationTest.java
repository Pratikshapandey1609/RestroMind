package com.restromind.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restromind.auth.dto.LoginRequest;
import com.restromind.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password123";

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);
        req.setFullName("Test User");
        req.setRole("USER");
        return req;
    }

    private String registerAndGetRefreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("refreshToken");
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("refreshToken");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        registerAndGetRefreshToken();
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_missingField_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(EMAIL);
        // missing password, fullName, role
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("not-an-email");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setPassword("short");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200() throws Exception {
        registerAndGetRefreshToken();
        LoginRequest req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        registerAndGetRefreshToken();
        LoginRequest req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("wrongpassword");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@example.com");
        req.setPassword(PASSWORD);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_missingField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returns200() throws Exception {
        String refreshToken = registerAndGetRefreshToken();
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_tamperedToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "tampered.token.here"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedAfterLogout_returns401() throws Exception {
        String refreshToken = registerAndGetRefreshToken();
        // logout to revoke
        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());
        // refresh should now fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ── full flow ─────────────────────────────────────────────────────────────

    @Test
    void fullFlow_register_login_refresh_logout_refresh401() throws Exception {
        // 1. register
        String refreshToken = registerAndGetRefreshToken();

        // 2. login
        String loginRefreshToken = loginAndGetRefreshToken();

        // 3. refresh with login token
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", loginRefreshToken))))
                .andExpect(status().isOk());

        // 4. logout (revokes all tokens for user id 1)
        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        // 5. refresh after logout → 401
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validRequest_returns200() throws Exception {
        registerAndGetRefreshToken();
        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }
}
