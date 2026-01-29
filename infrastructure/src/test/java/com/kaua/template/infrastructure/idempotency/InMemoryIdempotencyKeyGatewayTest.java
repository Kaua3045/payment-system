package com.kaua.template.infrastructure.idempotency;

import com.kaua.template.IntegrationTest;
import com.kaua.template.domain.utils.IdentifierUtils;
import com.kaua.template.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@IntegrationTest
public class InMemoryIdempotencyKeyGatewayTest {

    @Autowired
    private IdempotencyKeyGateway inMemoryIdempotencyKeyGateway;

    @Test
    void givenAValidValues_whenCallSave_thenSaveIdempotencyKey() {
        String idempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        long ttl = 1;
        TimeUnit timeUnit = TimeUnit.HOURS;

        Assertions.assertDoesNotThrow(() -> inMemoryIdempotencyKeyGateway.save(idempotencyKey, ttl, timeUnit));

        Assertions.assertTrue(inMemoryIdempotencyKeyGateway.find(idempotencyKey).isPresent());
    }

    @Test
    void givenAValidValues_whenCallSaveWithBody_thenSaveIdempotencyKey() {
        String idempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        long ttl = 1;
        TimeUnit timeUnit = TimeUnit.HOURS;
        IdempotencyKeyInput body = new IdempotencyKeyInput(
                200,
                "OK",
                Map.of("Location", "/api/hello")
        );

        Assertions.assertDoesNotThrow(() -> inMemoryIdempotencyKeyGateway.save(idempotencyKey, body, ttl, timeUnit));

        Assertions.assertTrue(inMemoryIdempotencyKeyGateway.find(idempotencyKey).isPresent());
    }

    @Test
    void givenAnInvalidIdempotencyKey_whenCallFind_thenReturnEmpty() {
        String idempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();

        Assertions.assertFalse(inMemoryIdempotencyKeyGateway.find(idempotencyKey).isPresent());
    }

    @Test
    void givenAValidIdempotencyKey_whenCallFindWithBody_thenReturnBody() {
        String idempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        long ttl = 1;
        TimeUnit timeUnit = TimeUnit.HOURS;
        IdempotencyKeyInput body = new IdempotencyKeyInput(
                200,
                "OK",
                Map.of("Location", "/api/hello")
        );

        Assertions.assertDoesNotThrow(() -> inMemoryIdempotencyKeyGateway.save(idempotencyKey, body, ttl, timeUnit));

        Assertions.assertTrue(inMemoryIdempotencyKeyGateway.find(idempotencyKey).isPresent());
    }
}