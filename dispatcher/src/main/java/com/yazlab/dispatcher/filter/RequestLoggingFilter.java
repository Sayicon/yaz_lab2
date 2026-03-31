package com.yazlab.dispatcher.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * FAZ 3-B: Her istek/yanıt çifti için Redis'e log yazar.
 * Log formatı: {"timestamp":"...","method":"...","path":"...","status":200,"latency":42}
 *
 * Redis dışlandığında (test profili) bu bean oluşturulmaz.
 */
@Component
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    public static final String LOG_KEY = "request-logs";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RequestLoggingFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return -50; // JwtAuthFilter (-100) sonrasında çalışır
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doFinally(signal -> {
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    int status = statusCode != null ? statusCode.value() : 0;
                    long latency = System.currentTimeMillis() - startTime;

                    String logEntry = "{\"timestamp\":\"" + Instant.now()
                            + "\",\"method\":\"" + method
                            + "\",\"path\":\"" + path
                            + "\",\"status\":" + status
                            + ",\"latency\":" + latency + "}";

                    redisTemplate.opsForList()
                            .rightPush(LOG_KEY, logEntry)
                            .subscribe();
                });
    }
}
