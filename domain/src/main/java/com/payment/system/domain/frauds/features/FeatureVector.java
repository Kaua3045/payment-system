package com.payment.system.domain.frauds.features;

import java.util.List;

public final class FeatureVector {

    // Index this is equals in training model
    public static final int LOG_AMOUNT = 0;
    public static final int AMOUNT_RATIO_LOG = 1;
    public static final int COUNT_1M_LOG = 2;
    public static final int COUNT_1H_LOG = 3;
    public static final int COUNT_24H_LOG = 4;
    public static final int DISTINCT_PAYEES_LOG = 5;
    public static final int NEW_PAYEE = 6;
    public static final int NIGHT = 7;
    public static final int COUNTERPARTY_NEW = 8; // Destination is NEW
    public static final int INSTRUMENT_RECENT = 9; // Pix, card, recently created
    public static final int COUNTRY_CHANGED = 10;
    public static final int SPEED_SCALED = 11;

    public static final List<String> NAMES = List.of(
            "log_amount", "amount_ratio_log",
            "count_1m_log", "count_1h_log", "count_24h_log", "distinct_payees_log",
            "new_payee", "night",
            "counterparty_new", "instrument_recent",
            "country_changed", "speed_scaled");

    public static final int SIZE = NAMES.size();

    private final double[] values;

    private FeatureVector(double[] values) {
        this.values = values;
    }

    // Throw on size incorrect and NaN
    public static FeatureVector of(double... v) {
        if (v.length != SIZE) {
            throw new IllegalArgumentException("requested " + SIZE + " features, received " + v.length);
        }
        double[] copy = v.clone();
        for (int i = 0; i < SIZE; i++) {
            // NaN/Infinity in feature make a fake compares
            if (Double.isNaN(copy[i]) || Double.isInfinite(copy[i])) {
                throw new IllegalArgumentException("feature invalid (NaN/Infinity): " + NAMES.get(i));
            }
        }
        return new FeatureVector(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    // Index comes from model already validated in parser
    public double get(int index) {
        return values[index];
    }

    // On non setted value set 0 to default
    public static final class Builder {
        private final double[] v = new double[SIZE];

        public Builder set(int index, double value) {
            v[index] = value;
            return this;
        }

        public FeatureVector build() {
            return of(v);
        }
    }
}
