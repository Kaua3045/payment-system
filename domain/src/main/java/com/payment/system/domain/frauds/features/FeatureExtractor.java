package com.payment.system.domain.frauds.features;

import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.utils.GeoUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static com.payment.system.domain.frauds.features.FeatureVector.*;

// Transform Attempt + Stats to FeatureVector: in this part velocity and geo location make numbers
public final class FeatureExtractor {

    private static final ZoneId BRT = ZoneId.of("America/Sao_Paulo");
    private static final long UNKNOWN_AGE = 9_999; // data empty = "very old" non penalizes

    public FeatureVector extract(final Attempt attempt, final Stats stats) {
        double amount = attempt.amount().amount().doubleValue();
        double avg = stats.avgAmount().amount().doubleValue();
        double ratio = avg > 0 ? Math.min(amount / avg, 50.0) : 1.0; // how much above for this account

        int hour = attempt.at().atZone(BRT).getHour();
        boolean night = hour >= 20 || hour < 6;

        long counterpartyAgeDays = attempt.destinationCreatedAt() == null ? UNKNOWN_AGE
                : Duration.between(attempt.destinationCreatedAt(), attempt.at()).toDays();
        Instant instrumentCreated = attempt.details().instrumentCreatedAt();      // Pix, card, null not apply
        long instrumentAgeHours = instrumentCreated == null ? UNKNOWN_AGE
                : Duration.between(instrumentCreated, attempt.at()).toHours();

        // Geo distancy
        double speedScaled = 0;
        boolean countryChanged = false;
        if (attempt.location() != null && stats.lastSeen() != null) {
            double km = GeoUtils.haversineKm(stats.lastSeen().point(), attempt.location());
            // 1-minute floor: With a time interval close to zero, any GPS/IP noise would result in “infinite speed.”
            double hours = Math.max(Duration.between(stats.lastSeen().at(), attempt.at()).toSeconds() / 3600.0, 1.0 / 60.0);
            speedScaled = Math.min((km / hours) / 1000.0, 1.0);          // 1.0 = 1000 km/h or plus
            String from = stats.lastSeen().point().countryCode(), to = attempt.location().countryCode();
            countryChanged = from != null && to != null && !from.equals(to);
        }

        return FeatureVector.builder()
                .set(LOG_AMOUNT, Math.log1p(amount))
                .set(AMOUNT_RATIO_LOG, Math.log1p(ratio))
                // VELOCITY
                .set(COUNT_1M_LOG, Math.log1p(stats.count1m()))
                .set(COUNT_1H_LOG, Math.log1p(stats.count1h()))
                .set(COUNT_24H_LOG, Math.log1p(stats.count24h()))
                .set(DISTINCT_PAYEES_LOG, Math.log1p(stats.distinctPayees24h()))
                // context
                .set(NEW_PAYEE, stats.knowPayee() ? 0 : 1)
                .set(NIGHT, night ? 1 : 0)
                .set(COUNTERPARTY_NEW, counterpartyAgeDays < 7 ? 1 : 0)
                .set(INSTRUMENT_RECENT, instrumentAgeHours < 24 ? 1 : 0)
                .set(COUNTRY_CHANGED, countryChanged ? 1 : 0)
                .set(SPEED_SCALED, speedScaled)
                .build();
    }
}
