package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InternalErrorExceptionTest extends UnitTest {

    @Test
    void givenAMessage_whenCallInternalErrorExceptionWith_ThenReturnInternalErrorException() {
        // given
        final var message = "Internal Error";

        // when
        final var internalErrorException = InternalErrorException.with(message);

        // then
        Assertions.assertEquals(message, internalErrorException.getMessage());
    }

    @Test
    void givenAMessageAndACause_whenCallInternalErrorExceptionWith_ThenReturnInternalErrorException() {
        // given
        final var message = "Internal Error";
        final var cause = new RuntimeException("Cause");

        // when
        final var internalErrorException = InternalErrorException.with(message, cause);

        // then
        Assertions.assertEquals(message, internalErrorException.getMessage());
        Assertions.assertEquals(cause, internalErrorException.getCause());
    }

    @Test
    void givenAMessageAndACauseAndAnErrorCode_whenCallInternalErrorExceptionWith_ThenReturnInternalErrorException() {
        // given
        final var message = "Internal Error";
        final var cause = new RuntimeException("Cause");
        final var errorCode = "internal_error";

        // when
        final var internalErrorException = InternalErrorException.with(message, cause, errorCode);

        // then
        Assertions.assertEquals(message, internalErrorException.getMessage());
        Assertions.assertEquals(cause, internalErrorException.getCause());
        Assertions.assertEquals(errorCode, internalErrorException.getErrorCode());
    }

    @Test
    void givenAMessageAndAnErrorCode_whenCallInternalErrorExceptionWith_ThenReturnInternalErrorException() {
        // given
        final var message = "Internal Error";
        final var errorCode = "internal_error";

        // when
        final var internalErrorException = InternalErrorException.with(message, errorCode);

        // then
        Assertions.assertEquals(message, internalErrorException.getMessage());
        Assertions.assertEquals(errorCode, internalErrorException.getErrorCode());
    }
}
