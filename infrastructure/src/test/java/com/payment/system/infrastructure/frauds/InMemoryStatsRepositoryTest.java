package com.payment.system.infrastructure.frauds;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.*;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.frauds.stats.InMemoryStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStatsRepositoryTest extends UnitTest {

    private static final Instant NOW =
            Instant.parse("2026-10-24T12:00:00Z");

    private static final GeoPoint PORTO_ALEGRE =
            new GeoPoint(-30.0346, -51.2177, "BR");

    private static final GeoPoint MANAUS =
            new GeoPoint(-3.1190, -60.0217, "BR");

    private static final Assessment APPROVED =
            new Assessment(Verdict.APPROVE, 0.0, List.of());

    private InMemoryStatsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStatsRepository();
    }

    @Test
    void shouldReturnEmptyStatsWhenNoEventsExist() {
        Stats stats = repository.load(attempt("query", NOW, null, "b"));

        assertEquals(0, stats.count1m());
        assertEquals(0, stats.count1h());
        assertEquals(0, stats.count24h());
        assertMoney("0", stats.sum1h());
        assertMoney("0", stats.sum24h());
        assertEquals(0, stats.distinctPayees24h());
        assertMoney("0", stats.avgAmount());
        assertFalse(stats.knowPayee());
        assertNull(stats.lastSeen());
    }

    @Test
    void shouldExcludeExpiredEventsFromTwentyFourHourWindow() {
        Attempt expired = attempt(
                "expired",
                NOW.minus(Duration.ofHours(24)).minusNanos(1),
                null,
                "b"
        );

        repository.record(expired, APPROVED);

        Stats stats = repository.load(
                attempt("query", NOW, null, "c")
        );

        assertEquals(0, stats.count24h());
        assertMoney("0", stats.sum24h());
        assertEquals(0, stats.distinctPayees24h());
    }

    @Test
    void shouldKeepEventExactlyAtTwentyFourHourBoundary() {
        Attempt boundary = attempt(
                "boundary",
                NOW.minus(Duration.ofHours(24)),
                null,
                "b"
        );

        repository.record(boundary, APPROVED);

        Stats stats = repository.load(
                attempt("query", NOW, null, "c")
        );

        assertEquals(1, stats.count24h());
        assertMoney("100", stats.sum24h());
    }

    @Test
    void shouldCountEventsAtOneHourAndOneMinuteBoundaries() {
        repository.record(
                attempt("one-minute", NOW.minus(1, ChronoUnit.MINUTES), null, "b"),
                APPROVED
        );
        repository.record(
                attempt("one-hour", NOW.minus(1, ChronoUnit.HOURS), null, "c"),
                APPROVED
        );
        repository.record(
                attempt("older", NOW.minus(2, ChronoUnit.HOURS), null, "d"),
                APPROVED
        );

        Stats stats = repository.load(
                attempt("query", NOW, null, "e")
        );

        assertEquals(1, stats.count1m());
        assertEquals(2, stats.count1h());
        assertEquals(3, stats.count24h());

        assertMoney("200", stats.sum1h());
        assertMoney("300", stats.sum24h());
    }

    @Test
    void shouldExcludeAttemptWithSameIdWhenLoadingStats() {
        repository.record(
                attempt("same-id", NOW, null, "b"),
                APPROVED
        );

        Stats stats = repository.load(
                attempt("same-id", NOW, null, "b")
        );

        assertEquals(0, stats.count1m());
        assertEquals(0, stats.count1h());
        assertEquals(0, stats.count24h());
        assertMoney("0", stats.sum24h());

        // The approved profile is independent of the event counters.
        assertMoney("100", stats.avgAmount());
        assertTrue(stats.knowPayee());
    }

    @Test
    void shouldIgnoreDuplicateAttemptWhenRecording() {
        Attempt first = attempt(
                "duplicate",
                NOW,
                PORTO_ALEGRE,
                "b",
                "100"
        );

        Attempt duplicate = attempt(
                "duplicate",
                NOW.plusSeconds(1),
                MANAUS,
                "c",
                "500"
        );

        repository.record(first, APPROVED);
        repository.record(duplicate, APPROVED);

        Stats stats = repository.load(
                attempt("query", NOW.plusSeconds(2), null, "d")
        );

        assertEquals(1, stats.count24h());
        assertEquals(1, stats.distinctPayees24h());
        assertMoney("100", stats.sum24h());
        assertMoney("100", stats.avgAmount());

        assertNotNull(stats.lastSeen());
        assertEquals(PORTO_ALEGRE, stats.lastSeen().point());
        assertEquals(NOW, stats.lastSeen().at());
    }

    @Test
    void shouldLearnFromApprovedAttempts() {
        repository.record(
                attempt("first", NOW, PORTO_ALEGRE, "b", "100"),
                APPROVED
        );
        repository.record(
                attempt("second", NOW.plusSeconds(1), PORTO_ALEGRE, "c", "200"),
                APPROVED
        );

        Stats stats = repository.load(
                attempt("query", NOW.plusSeconds(2), null, "b")
        );

        assertEquals(2, stats.count24h());
        assertEquals(2, stats.distinctPayees24h());
        assertMoney("300", stats.sum24h());
        assertMoney("150", stats.avgAmount());
        assertTrue(stats.knowPayee());
        assertNotNull(stats.lastSeen());
        assertEquals(NOW.plusSeconds(1), stats.lastSeen().at());
    }

    @Test
    void shouldNotLearnFromNonApprovedAttempts() {
        Verdict nonApproved = Arrays.stream(Verdict.values())
                .filter(verdict -> verdict != Verdict.APPROVE)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("A non-APPROVE verdict is required"));

        repository.record(
                attempt("denied", NOW, MANAUS, "b", "500"),
                new Assessment(nonApproved, 1.0, List.of())
        );

        Stats stats = repository.load(
                attempt("query", NOW.plusSeconds(1), null, "c")
        );

        // Every attempt contributes to velocity.
        assertEquals(1, stats.count24h());
        assertMoney("500", stats.sum24h());
        assertEquals(1, stats.distinctPayees24h());

        // Non-approved attempts must not train the trusted profile.
        assertMoney("0", stats.avgAmount());
        assertFalse(stats.knowPayee());
        assertNull(stats.lastSeen());
    }

    @Test
    void shouldPreserveLastSeenWhenApprovedAttemptHasNoLocation() {
        repository.record(
                attempt("located", NOW, PORTO_ALEGRE, "b"),
                APPROVED
        );

        repository.record(
                attempt("without-location",
                        NOW.plusSeconds(10), null, "c"),
                APPROVED
        );

        Stats stats = repository.load(
                attempt("query", NOW.plusSeconds(20), null, "d")
        );

        assertNotNull(stats.lastSeen());
        assertEquals(PORTO_ALEGRE, stats.lastSeen().point());
        assertEquals(NOW, stats.lastSeen().at());
    }

    @Test
    void shouldKeepLastSeenNullWhenApprovedAttemptHasNoLocation() {
        repository.record(
                attempt("without-location", NOW, null, "b"),
                APPROVED
        );

        Stats stats = repository.load(
                attempt("query", NOW.plusSeconds(1), null, "b")
        );

        assertNull(stats.lastSeen());
        assertMoney("100", stats.avgAmount());
        assertTrue(stats.knowPayee());
    }

    private static Attempt attempt(
            String id,
            Instant at,
            GeoPoint location,
            String destination
    ) {
        return attempt(id, at, location, destination, "100");
    }

    private static Attempt attempt(
            String id,
            Instant at,
            GeoPoint location,
            String destination,
            String amount
    ) {
        return Attempt.pix(
                id,
                "origin",
                destination,
                destination + "@pix",
                NOW.minus(Duration.ofDays(30)),
                Money.of(new BigDecimal(amount)),
                at,
                NOW.minus(Duration.ofDays(30)),
                new RequestContext("123.456.789.012")
        ).withLocation(location);
    }

    private static void assertMoney(String expected, Money actual) {
        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual.amount()),
                () -> "Expected " + expected + " but got " + actual
        );
    }
}