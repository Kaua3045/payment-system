package com.kaua.template.domain.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class IdentifierUtils {

    private IdentifierUtils() {}

    public static String generateNewId() {
        return UUID.randomUUID().toString();
    }

    public static String generateNewIdWithoutHyphen() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static UUID generateNewUUID() {
        return UUID.randomUUID();
    }

    public static ULID generateNewULID() {
        return ULID.random();
    }

    public static ULID generateNewMonotonicULID() {
        return MonotonicULID.random();
    }

    public static byte[] getUUIDAsBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    public static UUID bytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
}
