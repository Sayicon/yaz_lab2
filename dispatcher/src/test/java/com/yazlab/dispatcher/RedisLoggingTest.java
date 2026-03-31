package com.yazlab.dispatcher;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FAZ 3 - A: Redis log testi (TDD).
 *
 * Her istek/yanıt döngüsünde Dispatcher Redis'e log yazar.
 * Bu test Faz3-B'den önce commit'lenir ve Faz3-B implement edilene kadar başarısız olur.
 *
 * Beklenen davranış (Faz3-B'de uygulanacak):
 *  - Her istekte "request-logs" Redis listesine yeni bir entry eklenir.
 *  - Entry: JSON formatında {timestamp, method, path, status, latency, service}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class RedisLoggingTest {

    private static final String JWT_SECRET = "yazlab2-super-secret-key-change-in-prod-256bit";
    static final String REDIS_LOG_KEY = "request-logs";

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static MockWebServer userServiceMock;

    static {
        try {
            userServiceMock = new MockWebServer();
            userServiceMock.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Testcontainers Redis bağlantısı
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Servis URL'lerini mock'a yönlendir (main application.yml env var'larını override et)
        registry.add("USER_SERVICE_URL",
                () -> "http://localhost:" + userServiceMock.getPort());
        registry.add("AUTH_SERVICE_URL",
                () -> "http://localhost:" + userServiceMock.getPort());
        registry.add("PRODUCT_SERVICE_URL",
                () -> "http://localhost:" + userServiceMock.getPort());
    }

    @AfterAll
    static void teardown() throws IOException {
        userServiceMock.shutdown();
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ReactiveStringRedisTemplate redisTemplate;

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
    void afterRequest_redisLogEntry_shouldExist() throws InterruptedException {
        // Downstream mock'u hazırla
        userServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // İsteği gönder
        webTestClient.get()
                .uri("/users/")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        // Log'un Redis'e yazılması için kısa bekle (async)
        Thread.sleep(300);

        // Redis'te log kaydı olmalı (Faz3-B implement edilince geçecek)
        Long logCount = redisTemplate.opsForList().size(REDIS_LOG_KEY).block();
        assertThat(logCount)
                .as("Redis '%s' listesinde en az 1 log kaydı olmalı", REDIS_LOG_KEY)
                .isNotNull()
                .isGreaterThan(0);
    }

    @Test
    void logEntry_shouldContainMethodPathAndStatus() throws InterruptedException {
        userServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/users/1")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();

        Thread.sleep(300);

        // En son log kaydını al ve içeriğini doğrula (Faz3-B'de JSON formatı belirlenir)
        String latestLog = redisTemplate.opsForList().index(REDIS_LOG_KEY, -1).block();
        assertThat(latestLog)
                .as("Log kaydı null olmamalı")
                .isNotNull();
        assertThat(latestLog)
                .as("Log kaydı metod bilgisi içermeli")
                .containsIgnoringCase("GET");
        assertThat(latestLog)
                .as("Log kaydı path bilgisi içermeli")
                .contains("/users/");
    }
}
