package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsufficientFundsExceptionTest extends UnitTest {

    @Test
    void shouldCreateExceptionWithComplement() {
        final var exception = new InsufficientFundsException("Test complement");
        assertEquals("Insufficient funds Test complement", exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithoutComplement() {
        final var exception = new InsufficientFundsException();
        assertEquals("Insufficient funds", exception.getMessage());
    }
}
