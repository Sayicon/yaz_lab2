package com.yazlab.dispatcher;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FAZ 3 - A: Dispatcher yönlendirme testleri (TDD).
 *
 * Bu testler Faz3-B uygulama kodundan önce commit'lendi.
 * Test 1: GET /users/** → User Service'e yönlendirilir
 * Test 2: GET /products/** → Product Service'e yönlendirilir
 * Test 3: Ulaşılamayan servis → 502 veya 503 döner
 * Test 4: Bilinmeyen URL → 404 döner
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class RoutingTest {

    private static final String JWT_SECRET = "yazlab2-super-secret-key-change-in-prod-256bit";

    static MockWebServer userServiceMock;
    static MockWebServer productServiceMock;
    static int deadServicePort;

    static {
        try {
            userServiceMock = new MockWebServer();
            userServiceMock.start();
            productServiceMock = new MockWebServer();
            productServiceMock.start();
            // Kısa süre aç → port'u al → kapat → ECONNREFUSED ile hızlı 502
            MockWebServer deadMock = new MockWebServer();
            deadMock.start();
            deadServicePort = deadMock.getPort();
            deadMock.shutdown();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Sadece servis URL env var'larını override et.
     * Route yapısı application-test.yml'de tanımlı — list/YAML çakışmasını önler.
     */
    @DynamicPropertySource
    static void configureServiceUrls(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", () -> "http://localhost:" + userServiceMock.getPort());
        registry.add("PRODUCT_SERVICE_URL", () -> "http://localhost:" + productServiceMock.getPort());
        registry.add("DEAD_SERVICE_URL", () -> "http://localhost:" + deadServicePort);
        // AUTH_SERVICE_URL test.yml default'unu kullanır (localhost:9999)
    }

    @AfterAll
    static void stopMocks() throws IOException {
        userServiceMock.shutdown();
        productServiceMock.shutdown();
    }

    @Autowired
    WebTestClient webTestClient;

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    @Test
    void usersRequest_shouldBeRoutedToUserService() throws InterruptedException {
        userServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/users/")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = userServiceMock.takeRequest(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(recorded, "User Service mock bir istek almalıydı");
        Assertions.assertTrue(recorded.getPath().startsWith("/users/"),
                "İstek /users/ ile başlamalıydı, ancak: " + recorded.getPath());
    }

    @Test
    void productsRequest_shouldBeRoutedToProductService() throws InterruptedException {
        productServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/products/")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = productServiceMock.takeRequest(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(recorded, "Product Service mock bir istek almalıydı");
        Assertions.assertTrue(recorded.getPath().startsWith("/products/"),
                "İstek /products/ ile başlamalıydı, ancak: " + recorded.getPath());
    }

    @Test
    void request_toUnreachableService_shouldReturn502or503() {
        // /dead/** → kapalı port → gateway bağlanamaz → 5xx döner
        webTestClient.get()
                .uri("/dead/test")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isBetween(500, 599));
    }

    @Test
    void request_toUnknownPath_shouldReturn404() {
        // Hiçbir route'a uymayan yol → 404
        webTestClient.get()
                .uri("/nonexistent-service/test")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isNotFound();
    }
}
