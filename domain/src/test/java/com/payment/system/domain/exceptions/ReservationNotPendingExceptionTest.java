package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationNotPendingExceptionTest extends UnitTest {

    @Test
    void givenReservationNotPendingException_whenCreated_thenShouldHaveCorrectMessage() {
        final var exception = new ReservationNotPendingException();
        assertEquals("Reservation is not pending", exception.getMessage());
        assertEquals("reservation_not_pending", exception.getErrorCode());
    }
}
