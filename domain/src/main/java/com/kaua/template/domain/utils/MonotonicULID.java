package com.kaua.template.domain.utils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MonotonicULID {

    private final Random random;
    private final AtomicReference<byte[]> lastEntropy = new AtomicReference<>(new byte[ULID.ENTROPY_LENGTH]);
    private final AtomicLong lastTime = new AtomicLong(-1L);

    public MonotonicULID(final Random random) {
        this.random = random;
    }

    public ULID next(final long aNow) {
        byte[] entropy = new byte[ULID.ENTROPY_LENGTH];

        long previousTime = lastTime.getAndUpdate(prev -> Math.max(prev, aNow));
        if (previousTime == aNow) {
            // Increment entropy atomically
            entropy = incrementEntropy(lastEntropy.getAndUpdate(this::incrementEntropy));
        } else {
            // Generate new entropy for a new timestamp
            random.nextBytes(entropy);
            lastEntropy.set(entropy);
        }

        return ULID.generate(aNow, entropy);
    }

    private byte[] incrementEntropy(byte[] currentEntropy) {
        byte[] newEntropy = currentEntropy.clone();
        boolean carry = true;

        // Traverse bytes backwards (big-endian) and increment by 1
        for (int i = ULID.ENTROPY_LENGTH - 1; i >= 0; i--) {
            if (carry) {
                int value = (newEntropy[i] & 0xFF) + 1; // Unsigned addition
                newEntropy[i] = (byte) value; // Update actual byte value
                carry = value > 0xFF; // Check exists overflow
            }
        }

        if (carry) {
            throw new IllegalStateException("ULID entropy overflowed for the same millisecond");
        }

        return newEntropy;
    }

    public static MonotonicULID DEFAULT = new MonotonicULID(new SecureRandom());

    public static ULID random() {
        return DEFAULT.next(Instant.now().toEpochMilli());
    }
}
