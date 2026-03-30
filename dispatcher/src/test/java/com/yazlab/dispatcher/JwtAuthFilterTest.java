package com.yazlab.dispatcher;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * FAZ 2 - A: Dispatcher JWT doğrulama filtresi testleri.
 *
 * TDD gereği bu test, uygulama kodundan (Faz2-B) önce commit'lenir.
 * - Token olmadan gelen istek → 401
 * - Geçersiz token ile gelen istek → 401
 * - /auth/** yolları JWT kontrolünden muaf → 401 dönmez
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class JwtAuthFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String JWT_SECRET = "yazlab2-super-secret-key-change-in-prod-256bit";

    private String createValidToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    @Test
    void request_withNoAuthHeader_shouldReturn401() {
        webTestClient.get()
                .uri("/users/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void request_withInvalidToken_shouldReturn401() {
        webTestClient.get()
                .uri("/users/")
                .header("Authorization", "Bearer invalid.token.value")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void request_withExpiredToken_shouldReturn401() {
        // exp=1 (1970) → kesinlikle süresi dolmuş
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        webTestClient.get()
                .uri("/users/")
                .header("Authorization", "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void request_toAuthEndpoint_withNoToken_shouldNotReturn401() {
        // /auth/** JWT kontrolünden muaf; backend çalışmadığından 502/503 beklenebilir
        webTestClient.post()
                .uri("/auth/login")
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }
}
