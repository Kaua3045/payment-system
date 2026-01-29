package com.kaua.template.domain.utils;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class MonotonicULIDTest extends UnitTest {

    @Test
    void testGenerateMonotonicULIDSuccess() {
        final var aMonotonicUlid = MonotonicULID.random();

        Assertions.assertNotNull(aMonotonicUlid);
        Assertions.assertNotNull(aMonotonicUlid.toString());
    }

    @Test
    void testGenerateMonotonicULIDInSameMillisecond() {
        Runnable task = () -> {
            for (int i = 0; i <= 10; i++) {
                System.out.println(Thread.currentThread().getName() + " - " + MonotonicULID.random());
            }
        };

        // Create a thread pool with 2 threads
        try (final var aVirtualThreads = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("monotonic-ulid-test").factory()
        )) {
            final var aTask1 = aVirtualThreads.submit(task);
            final var aTask2 = aVirtualThreads.submit(task);

            // Wait for the threads to finish
            aTask1.get(10, TimeUnit.SECONDS);
            aTask2.get(10, TimeUnit.SECONDS);

            Assertions.assertTrue(true);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGenerateMonotonicULIDWithOverflowAndThrowException() throws Exception {
        final var expectedErrorMessage = "ULID entropy overflowed for the same millisecond";

        final var aMonotonicULID = new MonotonicULID(new SecureRandom());

        Field lastEntropy = MonotonicULID.class.getDeclaredField("lastEntropy");
        lastEntropy.setAccessible(true);

        Field lasTime = MonotonicULID.class.getDeclaredField("lastTime");
        lasTime.setAccessible(true);

        AtomicReference<byte[]> newEntropy = new AtomicReference<>(new byte[ULID.ENTROPY_LENGTH]);
        byte[] maxEntropy = new byte[16];
        // Define cada byte como 0xFF
        Arrays.fill(maxEntropy, (byte) 0xFF); // carrega o array com 0xFF
        newEntropy.set(maxEntropy);

        long now = Instant.now().toEpochMilli();
        AtomicLong newTime = new AtomicLong(now);

        lastEntropy.set(aMonotonicULID, newEntropy);
        lasTime.set(aMonotonicULID, newTime);

        final var aException = Assertions.assertThrows(IllegalStateException.class,
                () -> aMonotonicULID.next(now));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }
}
