package com.kaua.template.infrastructure.configurations;

import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import com.kaua.template.infrastructure.idempotency.gateways.InMemoryIdempotencyKeyGateway;
import com.kaua.template.infrastructure.idempotency.gateways.RedisIdempotencyKeyGateway;
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
