package com.payment.system.infrastructure.configurations;

import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import com.payment.system.infrastructure.idempotency.gateways.InMemoryIdempotencyKeyGateway;
import com.payment.system.infrastructure.idempotency.gateways.RedisIdempotencyKeyGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration(proxyBeanMethods = false)
public class IdempotencyKeyConfig {

    @Bean
    @ConditionalOnProperty(name = "idempotency-key.storage.type", havingValue = "in-memory")
    public IdempotencyKeyGateway inMemoryIdempotencyKeyGateway(final TracerWrapper tracerWrapper) {
        return new InMemoryIdempotencyKeyGateway(tracerWrapper);
    }

    @Bean
    @ConditionalOnProperty(name = "idempotency-key.storage.type", havingValue = "redis")
    public IdempotencyKeyGateway redisIdempotencyKeyGateway(final RedisTemplate<String, byte[]> redisTemplate, final TracerWrapper tracerWrapper) {
        return new RedisIdempotencyKeyGateway(redisTemplate, tracerWrapper);
    }
}
