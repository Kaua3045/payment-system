package com.kaua.template.infrastructure.idempotency;

import com.kaua.template.AbstractCacheConfig;
import com.kaua.template.ConcurrentlyTestHelper;
import com.kaua.template.IntegrationTest;
import com.kaua.template.ObservationTest;
import com.kaua.template.domain.utils.IdentifierUtils;
import com.kaua.template.infrastructure.exceptions.IdempotencyKeyAlreadyExistsException;
import com.kaua.template.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@IntegrationTest
@TestPropertySource(properties = {
        "idempotency-key.storage.type=REDIS"
})
class RedisIdempotencyKeyGatewayTest extends AbstractCacheConfig implements ObservationTest {

    private static final String IDEMPOTENCY_KEY_SAVE_WITH_BODY = "cache.idempotency_key.save_key_with_body";
    private static final String IDEMPOTENCY_KEY_SAVE = "cache.idempotency_key.save_key";
    private static final String IDEMPOTENCY_KEY_FIND = "cache.idempotency_key.find_key";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IdempotencyKeyGateway idempotencyKeyGateway;

    @Autowired
    private InMemorySpanExporter spanExporter;

    @BeforeEach
    void cleanUp() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
        resetSpans();
    }

    @Test
    void givenAValidIdempotencyKeyWithBody_whenSave_thenShouldSave() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        final var aBody = new IdempotencyKeyBodyTest("test");
        final var aHeaders = new HashMap<String, String>();
        aHeaders.put("location", "/test/123");
        aHeaders.put("content-type", MediaType.APPLICATION_JSON_VALUE);

        final var aIdempotencyKeyInput = new IdempotencyKeyInput(200, aBody, aHeaders);

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                aIdempotencyKeyInput,
                1,
                TimeUnit.HOURS
        );

        final var aResult = this.idempotencyKeyGateway.find(aIdempotencyKey).get();

        Assertions.assertNotNull(aResult.body());
        Assertions.assertEquals(aIdempotencyKeyInput.statusCode(), aResult.statusCode());
        Assertions.assertEquals(aIdempotencyKeyInput.headers().size(), aResult.headers().size());

        assertSpanCreated(IDEMPOTENCY_KEY_SAVE_WITH_BODY);
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE_WITH_BODY,
                "idempotency_key",
                aIdempotencyKey
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE_WITH_BODY,
                "ttl",
                1L
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE_WITH_BODY,
                "time_unit",
                TimeUnit.HOURS.name()
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE_WITH_BODY,
                "storage_type",
                "REDIS"
        );
    }

    @Test
    void givenAnExistsIdempotencyKey_whenSave_thenShouldThrowException() throws InterruptedException {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        final var aBody = new IdempotencyKeyBodyTest("test");

        final var aHeaders = new HashMap<String, String>();
        aHeaders.put("location", "/test/123");
        aHeaders.put("content-type", MediaType.APPLICATION_JSON_VALUE);

        final var aIdempotencyKeyInput = new IdempotencyKeyInput(200, aBody, aHeaders);

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                1,
                TimeUnit.HOURS
        );

        ConcurrentlyTestHelper.doSyncAndConcurrently(
                2,
                o -> this.idempotencyKeyGateway.save(
                        aIdempotencyKey,
                        aIdempotencyKeyInput,
                        1,
                        TimeUnit.HOURS
                ),
                RedisIdempotencyKeyGatewayTest.class.getSimpleName(),
                0,
                2
        );

        assertSpanCreated(IDEMPOTENCY_KEY_SAVE);
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "idempotency_key",
                aIdempotencyKey
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "ttl",
                1L
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "time_unit",
                TimeUnit.HOURS.name()
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "storage_type",
                "REDIS"
        );
    }

    @Test
    void givenAnExistsIdempotencyKey_whenGet_thenShouldReturn() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        final var aBody = new IdempotencyKeyBodyTest("test");

        final var aHeaders = new HashMap<String, String>();
        aHeaders.put("location", "/test/123");
        aHeaders.put("content-type", MediaType.APPLICATION_JSON_VALUE);

        final var aIdempotencyKeyInput = new IdempotencyKeyInput(200, String.valueOf(aBody), aHeaders);

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                aIdempotencyKeyInput,
                1,
                TimeUnit.HOURS
        );

        final var aResult = this.idempotencyKeyGateway.find(aIdempotencyKey).get();

        Assertions.assertNotNull(aResult.body());
        Assertions.assertEquals(aIdempotencyKeyInput.statusCode(), aResult.statusCode());
        Assertions.assertEquals(aIdempotencyKeyInput.headers().size(), aResult.headers().size());

        assertSpanCreated(IDEMPOTENCY_KEY_FIND);
        assertSpanAttribute(
                IDEMPOTENCY_KEY_FIND,
                "idempotency_key",
                aIdempotencyKey
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_FIND,
                "storage_type",
                "REDIS"
        );
    }

    @Test
    void givenAnNotExistsIdempotencyKey_whenGet_thenShouldReturnEmpty() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aResult = this.idempotencyKeyGateway.find(aIdempotencyKey);

        Assertions.assertTrue(aResult.isEmpty());

        assertSpanCreated(IDEMPOTENCY_KEY_FIND);
    }

    @Test
    void givenAValidIdempotencyKey_whenSave_thenShouldSave() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                1,
                TimeUnit.HOURS
        );

        final var aResult = this.idempotencyKeyGateway.find(aIdempotencyKey).get();

        Assertions.assertNotNull(aResult.body());
        Assertions.assertEquals(0, aResult.statusCode());
        Assertions.assertEquals(0, aResult.headers().size());

        assertSpanCreated(IDEMPOTENCY_KEY_SAVE);
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "idempotency_key",
                aIdempotencyKey
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "ttl",
                1L
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "time_unit",
                TimeUnit.HOURS.name()
        );
        assertSpanAttribute(
                IDEMPOTENCY_KEY_SAVE,
                "storage_type",
                "REDIS"
        );
    }

    @Test
    void testConcurrencySaveWithoutBody() throws InterruptedException {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        ConcurrentlyTestHelper.doSyncAndConcurrently(
                2,
                o -> this.idempotencyKeyGateway.save(
                        aIdempotencyKey,
                        1,
                        TimeUnit.HOURS
                ),
                RedisIdempotencyKeyGatewayTest.class.getSimpleName(),
                1,
                1
        );
    }

    @Test
    void givenAnExistsIdempotencyKeyWithoutBody_whenSave_thenReturnFalseAndThrowIdempotencyKeyAlreadyExists() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                1,
                TimeUnit.HOURS
        );

        Assertions.assertThrows(IdempotencyKeyAlreadyExistsException.class, () ->
                this.idempotencyKeyGateway.save(
                        aIdempotencyKey,
                        1,
                        TimeUnit.HOURS
                ));

        assertSpanCreated(IDEMPOTENCY_KEY_SAVE);
    }

    @Test
    void testSaveIdempotencyKeyWithBodyButWithoutOtherParams() {
        final var aIdempotencyKey = IdentifierUtils.generateNewIdWithoutHyphen();
        final var aBody = new IdempotencyKeyBodyTest("test");

        Assertions.assertTrue(this.idempotencyKeyGateway.find(aIdempotencyKey).isEmpty());

        this.idempotencyKeyGateway.save(
                aIdempotencyKey,
                new IdempotencyKeyInput(aBody),
                1,
                TimeUnit.HOURS
        );

        final var aResult = this.idempotencyKeyGateway.find(aIdempotencyKey).get();

        Assertions.assertNotNull(aResult.body());
        Assertions.assertEquals(0, aResult.statusCode());
        Assertions.assertEquals(0, aResult.headers().size());
    }

    @Override
    public InMemorySpanExporter getSpanExporter() {
        return spanExporter;
    }
}
