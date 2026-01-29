package com.kaua.template.infrastructure.idempotency.gateways;

import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.infrastructure.configurations.json.Json;
import com.kaua.template.infrastructure.exceptions.IdempotencyKeyAlreadyExistsException;
import com.kaua.template.infrastructure.idempotency.IdempotencyKeyDTO;
import com.kaua.template.infrastructure.idempotency.IdempotencyKeyInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RedisIdempotencyKeyGateway implements IdempotencyKeyGateway {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final String IDEMPOTENCY_SPAN_NAME = "cache.idempotency_key";

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyKeyGateway.class);

    private final RedisTemplate<String, byte[]> redisTemplate;
    private final TracerWrapper tracerWrapper;

    public RedisIdempotencyKeyGateway(final RedisTemplate<String, byte[]> redisTemplate, TracerWrapper tracerWrapper) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.tracerWrapper = tracerWrapper;
    }

    @Override
    public void save(final String idempotencyKey, final long ttl, final TimeUnit timeUnit) {
        this.tracerWrapper.trace(
                IDEMPOTENCY_SPAN_NAME.concat(".save_key"),
                (span) -> {
                    span.setAttribute("idempotency_key", idempotencyKey);
                    span.setAttribute("ttl", ttl);
                    span.setAttribute("time_unit", timeUnit.name());
                    span.setAttribute("storage_type", "REDIS");

                    final var aKey = IDEMPOTENCY_KEY_PREFIX.concat(idempotencyKey);

                    log.debug("Saving idempotency key: {}", aKey);

                    final var aSetResult = this.redisTemplate.opsForValue().setIfAbsent(
                            aKey,
                            Json.writeValueAsBytes(new IdempotencyKeyDTO(0, "", Map.of())),
                            ttl,
                            timeUnit
                    );

                    if (!Boolean.TRUE.equals(aSetResult)) {
                        throw new IdempotencyKeyAlreadyExistsException();
                    }

                    log.info("Idempotency key saved: {}", aKey);
                });
    }

    @Override
    public void save(final String idempotencyKey, final IdempotencyKeyInput body, final long ttl, final TimeUnit timeUnit) {
        this.tracerWrapper.trace(
                IDEMPOTENCY_SPAN_NAME.concat(".save_key_with_body"),
                (span) -> {
                    span.setAttribute("idempotency_key", idempotencyKey);
                    span.setAttribute("ttl", ttl);
                    span.setAttribute("time_unit", timeUnit.name());
                    span.setAttribute("storage_type", "REDIS");

                    final var aKey = IDEMPOTENCY_KEY_PREFIX.concat(idempotencyKey);

                    log.debug("Saving idempotency key with body {}", aKey);

                    final var aDTO = new IdempotencyKeyDTO(
                            body.statusCode(),
                            Json.writeValueAsString(body.body()),
                            body.headers()
                    );

                    final var aSetResult = this.redisTemplate.opsForValue().setIfAbsent(
                            aKey,
                            Json.writeValueAsBytes(aDTO),
                            ttl,
                            timeUnit
                    );

                    if (!Boolean.TRUE.equals(aSetResult)) {
                        throw new IdempotencyKeyAlreadyExistsException();
                    }

                    log.info("Idempotency key saved with body {}", aKey);
                }
        );
    }

    @Override
    public Optional<IdempotencyKeyDTO> find(final String idempotencyKey) {
        return this.tracerWrapper.traceWithReturn(
                IDEMPOTENCY_SPAN_NAME.concat(".find_key"),
                (span) -> {
                    span.setAttribute("idempotency_key", idempotencyKey);
                    span.setAttribute("storage_type", "REDIS");

                    final var aKey = IDEMPOTENCY_KEY_PREFIX.concat(idempotencyKey);

                    final var aResult = this.redisTemplate.opsForValue().get(aKey);

                    if (aResult == null) {
                        return Optional.empty();
                    }

                    final var aIdempotencyKeyBody = Json.readValue(aResult, IdempotencyKeyDTO.class);

                    log.debug("Idempotency key found: {}", aIdempotencyKeyBody);
                    return Optional.of(aIdempotencyKeyBody);
                }
        );
    }
}
