package com.yazlab.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * FAZ 1 - A: Dispatcher /health endpoint smoke testi.
 *
 * TDD gereği bu test, uygulama kodundan (Faz1-B) önce commit'lenir.
 * Test, Spring Boot Actuator'un /actuator/health endpoint'inin
 * HTTP 200 döndürdüğünü doğrular. Docker gerektirmez.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class HealthEndpointTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void healthEndpoint_shouldReturn200() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void healthEndpoint_shouldReturnUpStatus() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
