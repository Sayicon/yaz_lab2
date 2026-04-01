package com.yazlab.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FAZ 5 - A: Dispatcher /actuator/prometheus endpoint testi (TDD).
 *
 * Bu test Faz5-B'den önce commit'lenir.
 * micrometer-registry-prometheus bağımlılığı eklenmeden bu testler BAŞARISIZ olur.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class PrometheusEndpointTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void prometheusEndpoint_shouldReturn200() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void prometheusEndpoint_shouldReturnPrometheusTextFormat() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body)
                            .as("Prometheus formatında # HELP satırları olmalı")
                            .contains("# HELP");
                    assertThat(body)
                            .as("Prometheus formatında # TYPE satırları olmalı")
                            .contains("# TYPE");
                    assertThat(body)
                            .as("JVM metrikleri (jvm_) mevcut olmalı")
                            .contains("jvm_");
                });
    }
}
