package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModelFormatExceptionTest extends UnitTest {

    @Test
    void givenAnValidParams_whenCallsNewModelFormatException_thenShouldCreate() {
        final var message = "node invalid";
        final var line = 5;

        final var aException = new ModelFormatException(line, message);

        Assertions.assertEquals("Model invalid (line 5): node invalid", aException.getMessage());
        Assertions.assertEquals("FRAUD_MODEL_FORMAT_INVALID", aException.getErrorCode());
    }
}
