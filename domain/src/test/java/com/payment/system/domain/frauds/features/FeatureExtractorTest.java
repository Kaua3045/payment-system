package com.payment.system.domain.frauds.features;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.GeoPoint;
import com.payment.system.domain.frauds.RequestContext;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static com.payment.system.domain.frauds.features.FeatureVector.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureExtractorTest extends UnitTest {

    static final GeoPoint SP = new GeoPoint(-23.55, -46.63, "BR");
    static final GeoPoint MANAUS = new GeoPoint(-3.12, -60.02, "BR");
    static final GeoPoint LISBOA = new GeoPoint(38.72, -9.14, "PT");
    static final Instant NOON = Instant.parse("2026-09-30T15:00:00Z");    // 12:00 em Brasília
    static final Instant OLD = Instant.parse("2020-01-01T00:00:00Z");
    static final Money VALUE = Money.of(new BigDecimal("100.00"));

    final FeatureExtractor fx = new FeatureExtractor();

    Attempt attempt(Instant at, GeoPoint loc, Instant destCreated, Instant keyCreated) {
        return Attempt.pix("id", "maria", "joao", "joao@pix", keyCreated, Money.of(new BigDecimal(10_000)), at, destCreated, new RequestContext("216126736712")).withLocation(loc);
    }

    Stats seenAt(GeoPoint place, Instant when, boolean knownPayee) {
        return new Stats(0, 0, 0, Money.zero(), Money.zero(), 0, Money.zero(), knownPayee, new Stats.LastSeen(place, when));
    }

    @Test
    void impossibleTravelSaturatesSpeed() {
        FeatureVector f = fx.extract(attempt(NOON.plus(Duration.ofMinutes(10)), MANAUS, OLD, OLD), seenAt(SP, NOON, true));
        assertEquals(1.0, f.get(SPEED_SCALED), 0.0);
    }

    @Test
    void samePlaceHasNoSpeed() {
        FeatureVector f = fx.extract(attempt(NOON.plus(Duration.ofMinutes(10)), SP, OLD, OLD), seenAt(SP, NOON, true));
        assertEquals(0.0, f.get(SPEED_SCALED), 1e-9);
        assertEquals(0.0, f.get(COUNTRY_CHANGED), 0.0);
    }

    @Test
    void sameTripThirtyHoursLaterIsPlausible() {
        FeatureVector f = fx.extract(attempt(NOON.plus(Duration.ofHours(30)), MANAUS, OLD, OLD), seenAt(SP, NOON, true));
        assertTrue(f.get(SPEED_SCALED) < 0.2, "velocidade escalada: " + f.get(SPEED_SCALED));
    }

    @Test
    void countryChangeIsFlagged() {
        FeatureVector f = fx.extract(attempt(NOON.plus(Duration.ofHours(30)), LISBOA, OLD, OLD), seenAt(SP, NOON, true));
        assertEquals(1.0, f.get(COUNTRY_CHANGED), 0.0);
    }

    @Test
    void nightWindowUsesBrasiliaTime() {
        Stats s = Stats.empty();
        assertEquals(0.0, fx.extract(attempt(NOON, null, OLD, OLD), s).get(NIGHT), 0.0);
        assertEquals(1.0, fx.extract(attempt(Instant.parse("2026-10-01T02:30:00Z"), null, OLD, OLD), s).get(NIGHT), 0.0);  // 23:30 BRT
    }

    @Test
    void instrumentRecentIsNeutralAcrossChannels() {
        Instant twoHoursAgo = NOON.minus(Duration.ofHours(2));
        Attempt pixKey = Attempt.pix("p1", "maria", "joao", "joao@pix", twoHoursAgo, VALUE, NOON, OLD, new RequestContext("2793712687316287"));
        Attempt pixKeyTwo = Attempt.pix("p1", "maria", "joao", "joao@pix", OLD, VALUE, NOON, OLD, new RequestContext("2793712687316287"));
        assertEquals(1.0, fx.extract(pixKey, Stats.empty()).get(INSTRUMENT_RECENT), 0.0); // pix new
        assertEquals(0.0, fx.extract(pixKeyTwo, Stats.empty()).get(INSTRUMENT_RECENT), 0.0);
    }

    @Test
    void earlyMorningUsesBrasiliaTime() {
        // 03:30 em Brasília
        Instant earlyMorning = Instant.parse("2026-10-17T06:30:00Z");

        FeatureVector f = fx.extract(
                attempt(earlyMorning, null, OLD, OLD),
                Stats.empty()
        );

        assertEquals(1.0, f.get(NIGHT), 0.0);
    }

    @Test
    void missingCounterpartyCreationDateUsesUnknownAge() {
        FeatureVector f = fx.extract(
                attempt(NOON, null, null, OLD),
                Stats.empty()
        );

        assertEquals(0.0, f.get(COUNTERPARTY_NEW), 0.0);
    }

    @Test
    void recentCounterpartyIsFlaggedAsNew() {
        Instant createdThreeDaysAgo = NOON.minus(Duration.ofDays(3));

        FeatureVector f = fx.extract(
                attempt(NOON, null, createdThreeDaysAgo, OLD),
                Stats.empty()
        );

        assertEquals(1.0, f.get(COUNTERPARTY_NEW), 0.0);
    }

    @Test
    void missingLocationDoesNotCalculateGeographicSpeed() {
        FeatureVector f = fx.extract(
                attempt(NOON, null, OLD, OLD),
                seenAt(SP, NOON, true)
        );

        assertEquals(0.0, f.get(SPEED_SCALED), 0.0);
        assertEquals(0.0, f.get(COUNTRY_CHANGED), 0.0);
    }

    @Test
    void missingLastSeenDoesNotCalculateGeographicSpeed() {
        FeatureVector f = fx.extract(
                attempt(NOON, SP, OLD, OLD),
                Stats.empty()
        );

        assertEquals(0.0, f.get(SPEED_SCALED), 0.0);
        assertEquals(0.0, f.get(COUNTRY_CHANGED), 0.0);
    }

    @Test
    void missingInstrumentCreationDateUsesUnknownAge() {
        Attempt pixKey = Attempt.pix(
                "p1", "maria", "joao", "joao@pix",
                null, VALUE, NOON, OLD, new RequestContext("2793712687316287")
        );

        FeatureVector f = fx.extract(pixKey, Stats.empty());

        assertEquals(0.0, f.get(INSTRUMENT_RECENT), 0.0);
    }


    @Test
    void missingOriginCountryDoesNotFlagCountryChange() {
        GeoPoint originWithoutCountry = new GeoPoint(-23.55, -46.63, null);
        GeoPoint destination = LISBOA;

        FeatureVector f = fx.extract(
                attempt(NOON.plus(Duration.ofHours(30)), destination, OLD, OLD),
                seenAt(originWithoutCountry, NOON, true)
        );

        assertEquals(0.0, f.get(COUNTRY_CHANGED), 0.0);
    }

    @Test
    void missingDestinationCountryDoesNotFlagCountryChange() {
        GeoPoint destinationWithoutCountry = new GeoPoint(-23.55, -46.63, null);

        FeatureVector f = fx.extract(
                attempt(NOON.plus(Duration.ofHours(30)), destinationWithoutCountry, OLD, OLD),
                seenAt(SP, NOON, true)
        );

        assertEquals(0.0, f.get(COUNTRY_CHANGED), 0.0);
    }


    @Test
    void positiveAverageAmountCalculatesAmountRatio() {
        Stats stats = new Stats(
                0, 0, 0,
                Money.zero(), Money.zero(),
                0,
                Money.of(new BigDecimal("100.00")),
                false,
                null
        );

        Attempt attempt = Attempt.pix(
                "ratio-test", "maria", "joao", "joao@pix",
                OLD, Money.of(new BigDecimal("200.00")), NOON, OLD, new RequestContext("2793712687316287")
        );

        FeatureVector f = fx.extract(attempt, stats);

        assertEquals(
                Math.log1p(2.0),
                f.get(AMOUNT_RATIO_LOG),
                1e-9
        );
    }

    @Test
    void amountRatioIsCappedAtFifty() {
        Stats stats = new Stats(
                0, 0, 0,
                Money.zero(), Money.zero(),
                0,
                Money.of(new BigDecimal("10.00")),
                false,
                null
        );

        Attempt attempt = Attempt.pix(
                "ratio-cap-test", "maria", "joao", "joao@pix",
                OLD, Money.of(new BigDecimal("1000.00")), NOON, OLD, new RequestContext("2793712687316287")
        );

        FeatureVector f = fx.extract(attempt, stats);

        assertEquals(
                Math.log1p(50.0),
                f.get(AMOUNT_RATIO_LOG),
                1e-9
        );
    }
}
