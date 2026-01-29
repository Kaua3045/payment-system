package com.kaua.template.infrastructure.idempotency.gateways;

import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.infrastructure.configurations.json.Json;
import com.kaua.template.infrastructure.idempotency.IdempotencyKeyDTO;
import com.kaua.template.infrastructure.idempotency.IdempotencyKeyInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryIdempotencyKeyGateway implements IdempotencyKeyGateway {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyKeyGateway.class);
    private static final String IDEMPOTENCY_SPAN_NAME = "cache.idempotency_key";

    private final ConcurrentHashMap<String, String> idempotencyKeyMap;

    private final TracerWrapper tracerWrapper;

    public InMemoryIdempotencyKeyGateway(TracerWrapper tracerWrapper) {
        this.idempotencyKeyMap = new ConcurrentHashMap<>();
        this.tracerWrapper = tracerWrapper;
    }

    @Override
    public void save(final String idempotencyKey, final long ttl, final TimeUnit timeUnit) {
        this.tracerWrapper.trace(
                IDEMPOTENCY_SPAN_NAME.concat(".save_key"),
                (span) -> {
                    span.setAttribute("idempotency", idempotencyKey);
                    span.setAttribute("ttl", ttl);
                    span.setAttribute("time_unit", timeUnit.name());
                    span.setAttribute("storage_type", "IN_MEMORY");

                    log.debug("Saving idempotency key: {}", idempotencyKey);

                    this.idempotencyKeyMap.put(idempotencyKey, Json.writeValueAsString(
                            new IdempotencyKeyDTO(0, "", Map.of())
                    ));

                    log.info("Idempotency key saved: {}", idempotencyKey);
                }
        );
    }

    @Override
    public void save(final String idempotencyKey, final IdempotencyKeyInput body, final long ttl, final TimeUnit timeUnit) {
        this.tracerWrapper.trace(
                IDEMPOTENCY_SPAN_NAME.concat(".save_key_with_body"),
                (span) -> {
                    span.setAttribute("idempotency", idempotencyKey);
                    span.setAttribute("ttl", String.valueOf(ttl));
                    span.setAttribute("time_unit", timeUnit.name());
                    span.setAttribute("storage_type", "IN_MEMORY");

                    log.debug("Saving idempotency key with body: {}", idempotencyKey);
                    this.idempotencyKeyMap.put(idempotencyKey, Json.writeValueAsString(body));
                    log.info("Idempotency key saved with body: {}", idempotencyKey);
                }
        );
    }

    @Override
    public Optional<IdempotencyKeyDTO> find(final String idempotencyKey) {
        return this.tracerWrapper.traceWithReturn(
                IDEMPOTENCY_SPAN_NAME.concat(".find_key"),
                (span) -> {
                    span.setAttribute("idempotency", idempotencyKey);
                    span.setAttribute("storage_type", "IN_MEMORY");

                    return Optional.ofNullable(this.idempotencyKeyMap.get(idempotencyKey))
                            .map(body -> Json.readValue(body, IdempotencyKeyDTO.class));
                }
        );
    }
}
