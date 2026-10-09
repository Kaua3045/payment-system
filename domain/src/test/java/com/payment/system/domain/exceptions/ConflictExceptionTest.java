package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.validation.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConflictExceptionTest extends UnitTest {

    @Test
    void givenAValidMessage_whenCallConflictExceptionWith_ThenReturnConflictException() {
        var message = "Conflict Error";
        var errorCode = "conflict_error";

        var conflictException = ConflictException.with(message, errorCode);

        Assertions.assertEquals(message, conflictException.getMessage());
        Assertions.assertEquals(errorCode, conflictException.getErrorCode());
    }

    @Test
    void givenAValidListOfError_whenCallConflictExceptionWith_ThenReturnConflictException() {
        var errors = List.of(new Error("sample", "Conflict Error"));
        var errorCode = "conflict_error";

        var conflictException = ConflictException.with(errors, errorCode);

        Assertions.assertEquals(errors, conflictException.getErrors());
        Assertions.assertEquals(errorCode, conflictException.getErrorCode());
    }
}
