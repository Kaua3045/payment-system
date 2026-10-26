package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StatsTest extends UnitTest {

    @Test
    void givenNotValues_whenCallEmpty_shouldReturnEmptyStats() {
        final var aEmpty = Stats.empty();

        Assertions.assertEquals(0, aEmpty.count1m());
        Assertions.assertEquals(0, aEmpty.count1h());
        Assertions.assertEquals(0, aEmpty.count24h());
        Assertions.assertEquals(Money.zero(), aEmpty.sum1h());
        Assertions.assertEquals(Money.zero(), aEmpty.sum24h());
        Assertions.assertEquals(0, aEmpty.distinctPayees24h());
        Assertions.assertEquals(Money.zero(), aEmpty.avgAmount());
        Assertions.assertFalse(aEmpty.knowPayee());
        Assertions.assertNull(aEmpty.lastSeen());
    }
}
