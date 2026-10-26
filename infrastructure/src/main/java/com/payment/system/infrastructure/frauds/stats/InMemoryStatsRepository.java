package com.payment.system.infrastructure.frauds.stats;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.valueobjects.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStatsRepository implements StatsRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryStatsRepository.class);

    private record Event(String id, Instant at, BigDecimal amount, String payee) {
    }

    private static final class State {
        final Deque<Event> events = new ArrayDeque<>();
        final Set<String> knowPayees = new HashSet<>();
        long approvedCount;
        BigDecimal approvedSum = BigDecimal.ZERO;
        Stats.LastSeen lastSeen;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();

    @Override
    public Stats load(final Attempt attempt) {
        log.debug("InMemoryStatsRepository Loading origin stats account for attempt {}", attempt);
        final var state = states.computeIfAbsent(attempt.origin(), k -> new State());

        synchronized (state) {
            final var now = attempt.at();
            // TODO check this
            while (!state.events.isEmpty() && state.events.peekFirst().at().isBefore(now.minus(Duration.ofHours(24)))) {
                state.events.pollFirst();
            }

            int count1m = 0, count1h = 0, count24h = 0;
            BigDecimal sum1h = BigDecimal.ZERO, sum24h = BigDecimal.ZERO;
            Set<String> payees = new HashSet<>();
            for (Event e : state.events) {
                if (e.id().equals(attempt.id())) continue;

                Duration age = Duration.between(e.at(), now);
                count24h++;
                sum24h = sum24h.add(e.amount());
                payees.add(e.payee());

                if (age.compareTo(Duration.ofHours(1)) <= 0) {
                    count1h++;
                    sum1h = sum1h.add(e.amount());
                }
                if (age.compareTo(Duration.ofMinutes(1)) <= 0) {
                    count1m++;
                }
            }

            BigDecimal avg = state.approvedCount == 0 ? BigDecimal.ZERO
                    : state.approvedSum.divide(BigDecimal.valueOf(state.approvedCount), 2, RoundingMode.HALF_EVEN);

            final var stats = new Stats(count1m, count1h, count24h, Money.of(sum1h), Money.of(sum24h), payees.size(), Money.of(avg),
                    state.knowPayees.contains(attempt.destination()),
                    state.lastSeen
            );
            log.info("InMemoryStatsRepository loaded stats for account origin {}", stats);
            return stats;
        }
    }

    @Override
    public void record(final Attempt attempt, final Assessment result) {
        log.debug("InMemoryStatsRepository record attempt {} with assessment {}", attempt, result);
        final var state = states.computeIfAbsent(attempt.origin(), k -> new State());
        synchronized (state) {
            if (state.events.stream().anyMatch(e -> e.id().equals(attempt.id()))) return; // independent for id

            // Every attempt counts towards velocity (including denied attempts: anyone who tries 20 times is suspicious)...
            state.events.addLast(new Event(attempt.id(), attempt.at(), attempt.amount().amount(), attempt.destination()));
            // ...but we only LEARN confidence from those who have been approved (otherwise the fraudster "trains" the victim's profile).
            // This also applies to location: a suspicious attempt does NOT move the "last trusted location".
            // (otherwise the first attempt from Manaus would clear up the impossible trip and the second would pass).
            if (result.verdict() == Verdict.APPROVE) {
                state.knowPayees.add(attempt.destination());
                state.approvedCount++;
                state.approvedSum = state.approvedSum.add(attempt.amount().amount());
                if (attempt.location() != null) {
                    state.lastSeen = new Stats.LastSeen(attempt.location(), attempt.at());
                }
            }
        }
    }
}
