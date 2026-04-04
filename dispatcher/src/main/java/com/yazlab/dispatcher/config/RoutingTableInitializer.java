package com.yazlab.dispatcher.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FAZ 3-B: Uygulama başlarken routing tablosunu Redis'e yazar.
 * Redis hash anahtarı: "routing-table"
 * Format: { "user-service" -> "http://user-service:8082", ... }
 *
 * Bu tablo Dispatcher'ın hangi servisin nerede olduğunu bilmesini sağlar.
 * Redis dışlandığında (test profili) redisTemplate null olur, yazma atlanır.
 */
@Component
public class RoutingTableInitializer implements ApplicationRunner {

    public static final String ROUTING_TABLE_KEY = "routing-table";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, String> routingTable;

    public RoutingTableInitializer(
            @Autowired(required = false) ReactiveStringRedisTemplate redisTemplate,
            @Value("${AUTH_SERVICE_URL:http://auth-service:8081}") String authServiceUrl,
            @Value("${USER_SERVICE_URL:http://user-service:8082}") String userServiceUrl,
            @Value("${PRODUCT_SERVICE_URL:http://product-service:8083}") String productServiceUrl) {
        this.redisTemplate = redisTemplate;
        this.routingTable = Map.of(
                "auth-service", authServiceUrl,
                "user-service", userServiceUrl,
                "product-service", productServiceUrl
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        if (redisTemplate == null) return;
        redisTemplate.opsForHash()
                .putAll(ROUTING_TABLE_KEY, routingTable)
                .subscribe();
    }
}
