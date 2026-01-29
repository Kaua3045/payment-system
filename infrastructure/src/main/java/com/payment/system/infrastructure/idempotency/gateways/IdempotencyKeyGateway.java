package com.payment.system.infrastructure.idempotency.gateways;

import com.payment.system.infrastructure.idempotency.IdempotencyKeyDTO;
import com.payment.system.infrastructure.idempotency.IdempotencyKeyInput;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface IdempotencyKeyGateway {

    void save(String idempotencyKey, long ttl, TimeUnit timeUnit);

    void save(String idempotencyKey, IdempotencyKeyInput body, long ttl, TimeUnit timeUnit);

    Optional<IdempotencyKeyDTO> find(String idempotencyKey);
}
