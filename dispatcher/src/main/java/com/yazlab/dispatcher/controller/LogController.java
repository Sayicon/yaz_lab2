package com.yazlab.dispatcher.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * FAZ 5-B: Dispatcher'daki Redis log listesini dışa açan endpoint.
 * GET /api/logs?limit=50 → son N log kaydını JSON string dizisi olarak döner.
 *
 * Bu endpoint Gateway routing'ine dahil değil; @RestController olarak doğrudan
 * WebFlux tarafından sunulur (JwtAuthFilter uygulanmaz).
 *
 * Redis dışlandığında (test profili) bu bean oluşturulmaz.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class LogController {

    private final ReactiveStringRedisTemplate redisTemplate;

    public LogController(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/logs")
    public Mono<ResponseEntity<List<String>>> getLogs(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return redisTemplate.opsForList()
                .range("request-logs", -limit, -1)
                .collectList()
                .map(ResponseEntity::ok);
    }
}
