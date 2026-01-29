package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.validation.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValidationExceptionTest extends UnitTest {

    @Test
    void givenAValidError_whenCreateValidationException_shouldReturnMessage() {
        final var aMessage = "Validation error";
        final var aException = ValidationException.with(new Error(aMessage));

        final var aResult = aException.getErrors().getFirst().message();

        Assertions.assertEquals(aMessage, aResult);
    }

    @Test
    void givenAValidError_whenCreateValidationException_shouldReturnError() {
        final var aMessage = "Validation error";
        final var aException = ValidationException.with(List.of(new Error(aMessage)));

        final var aResult = aException.getErrors().getFirst().message();

        Assertions.assertEquals(aMessage, aResult);
    }
}
