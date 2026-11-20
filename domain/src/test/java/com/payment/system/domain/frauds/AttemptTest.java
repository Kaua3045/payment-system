package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

class AttemptTest extends UnitTest {

    @Test
    void givenAnValidValues_whenWithLocation_shouldBeOk() {
        final var aAttempt = Attempt.pix(
                "idempotencyKey1234",
                "originId12312312",
                "destinationId12312237213",
                "johndoe@gmail.com",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("100.00")),
                InstantUtils.now(),
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("123.456.789.012")
        );

        final var aAttemptWithLocation = aAttempt.withLocation(new GeoPoint(-23.5505, -46.6333, "BR"));

        Assertions.assertEquals("idempotencyKey1234", aAttemptWithLocation.id());
        Assertions.assertEquals("originId12312312", aAttemptWithLocation.origin());
        Assertions.assertEquals("destinationId12312237213", aAttemptWithLocation.destination());
        Assertions.assertNotNull(aAttemptWithLocation.details());
        Assertions.assertNotNull(aAttemptWithLocation.amount());
        Assertions.assertNotNull(aAttemptWithLocation.at());
        Assertions.assertNotNull(aAttemptWithLocation.location());
        Assertions.assertNotNull(aAttemptWithLocation.destinationCreatedAt());
        Assertions.assertEquals(Channel.PIX, aAttemptWithLocation.channel());
    }

    @Test
    void givenAnValidValuesWithContext_whenCreatePixAttempt_shouldBeOk() {
        final var aAttempt = Attempt.pix(
                "idempotencyKey1234",
                "originId12312312",
                "destinationId12312237213",
                "johndoe@gmail.com",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("100.00")),
                InstantUtils.now(),
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("123.456.789.012")
        );


        Assertions.assertEquals("idempotencyKey1234", aAttempt.id());
        Assertions.assertEquals("originId12312312", aAttempt.origin());
        Assertions.assertEquals("destinationId12312237213", aAttempt.destination());
        Assertions.assertNotNull(aAttempt.details());
        Assertions.assertNotNull(aAttempt.amount());
        Assertions.assertNotNull(aAttempt.at());
        Assertions.assertNull(aAttempt.location());
        Assertions.assertNotNull(aAttempt.destinationCreatedAt());
        Assertions.assertEquals(Channel.PIX, aAttempt.channel());
    }
}
