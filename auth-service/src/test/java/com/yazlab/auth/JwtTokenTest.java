package com.yazlab.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FAZ 2 - A: JWT token doğrulama testleri.
 *
 * TDD gereği bu test, uygulama kodundan (Faz2-B) önce commit'lenir.
 * - Geçerli token → /auth/validate 200 + doğru payload döner.
 * - Süresi dolmuş / geçersiz token → /auth/validate 401 döner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Geçerli token: register → login → token al → validate → 200 + doğru username döner.
     */
    @Test
    void validateToken_withValidToken_shouldReturn200AndCorrectPayload() throws Exception {
        // Register
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "jwtuser", "password": "pass123", "email": "jwt@test.com"}
                                """))
                .andExpect(status().isCreated());

        // Login → token al
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "jwtuser", "password": "pass123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(token).isNotBlank();

        // Validate
        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jwtuser"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    /**
     * Süresi dolmuş / sahte token → validate 401 döner.
     */
    @Test
    void validateToken_withExpiredToken_shouldReturn401() throws Exception {
        // exp=1 (1970) ile imzalanmış geçersiz token
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + expiredToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tamamen bozuk token → validate 401 döner.
     */
    @Test
    void validateToken_withMalformedToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"not.a.valid.jwt\"}"))
                .andExpect(status().isUnauthorized());
    }
}
