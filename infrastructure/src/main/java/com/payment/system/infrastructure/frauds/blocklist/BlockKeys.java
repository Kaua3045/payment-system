package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.exceptions.InternalErrorException;

import java.util.Set;

public final class BlockKeys {

    private static final Set<String> KINDS = Set.of("acc", "key");

    private BlockKeys() {}

    public static String require(final String raw) {
        final var key = raw == null ? "" : raw.trim();
        int colon = key.indexOf(':');
        if (colon <= 0 || colon == key.length() - 1) {
            throw InternalErrorException.with("Key invalid '" + key + "' use type:value (types: " + KINDS + ")");
        }
        final var kind = key.substring(0, colon);
        if (!KINDS.contains(kind)) {
            throw InternalErrorException.with("Type not found '" + kind + "' in '" + key + "' (types: " + KINDS + ")");
        }
        return key;
    }
}
