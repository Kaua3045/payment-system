package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FraudDenyExceptionTest extends UnitTest {

    @Test
    void givenAnMessage_whenCallsNewFraudDenyException_thenShouldBeOK() {
        final var aMessage = "Transaction rejected by fraud analysis";

        final var aException = new FraudDenyException(aMessage);

        Assertions.assertEquals(aMessage, aException.getMessage());
        Assertions.assertEquals("FRAUD_DENY", aException.getErrorCode());
    }
}
