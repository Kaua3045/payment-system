package com.payment.system.domain.frauds;

import com.payment.system.domain.valueobjects.Money;

import java.time.Instant;

// This class contains raw history for origin account
public record Stats(
        int count1m,
        int count1h,
        int count24h,
        Money sum1h,
        Money sum24h,
        int distinctPayees24h,
        Money avgAmount,
        boolean knowPayee,
        LastSeen lastSeen
) {

    public record LastSeen(GeoPoint point, Instant at) {
    }

    public static Stats empty() {
        return new Stats(
                0,
                0,
                0,
                Money.zero(),
                Money.zero(),
                0,
                Money.zero(),
                false,
                null
        );
    }
}
