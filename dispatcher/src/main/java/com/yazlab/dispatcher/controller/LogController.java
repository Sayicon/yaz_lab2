package com.yazlab.dispatcher.controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FAZ 5-B / 6.5: Dispatcher'daki Redis log listesini dışa açan endpoint.
 * GET /api/logs?limit=50 → son N log kaydını JSON string dizisi olarak döner.
 *
 * Bu endpoint Gateway routing'ine dahil değil; @RestController olarak doğrudan
 * WebFlux tarafından sunulur (JwtAuthFilter uygulanmaz). Bu nedenle JWT doğrulama
 * manuel olarak yapılır.
 *
 * Redis dışlandığında (test profili) redisTemplate null olur, boş liste döner.
 */
@RestController
@RequestMapping("/api")
public class LogController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final SecretKey secretKey;

    public LogController(
            @Autowired(required = false) ReactiveStringRedisTemplate redisTemplate,
            @Value("${jwt.secret:}") String secret) {
        this.redisTemplate = redisTemplate;
        this.secretKey = secret.isEmpty() ? null : Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/logs")
    public Mono<ResponseEntity<List<String>>> getLogs(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (secretKey != null) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            try {
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(authHeader.substring(7));
            } catch (JwtException | IllegalArgumentException e) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
        }

        if (redisTemplate == null) {
            return Mono.just(ResponseEntity.ok(List.of()));
        }
        return redisTemplate.opsForList()
                .range("request-logs", -limit, -1)
                .collectList()
                .map(ResponseEntity::ok);
    }
}
