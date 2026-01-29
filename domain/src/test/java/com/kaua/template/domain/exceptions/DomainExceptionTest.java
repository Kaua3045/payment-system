package com.kaua.template.domain.exceptions;

import com.kaua.template.domain.UnitTest;
import com.kaua.template.domain.validation.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DomainExceptionTest extends UnitTest {

    @Test
    void givenAValidListOfError_whenCallDomainExceptionWith_ThenReturnDomainException() {
        var errors = List.of(new Error("sample", "Common Error"));

        var domainException = DomainException.with(errors);

        Assertions.assertEquals(errors, domainException.getErrors());
    }

    @Test
    void givenAValidError_whenCallDomainExceptionWith_ThenReturnDomainException() {
        var error = new Error("Common Error");

        var domainException = DomainException.with(error);

        Assertions.assertEquals(List.of(error), domainException.getErrors());
    }

    @Test
    void givenAValidListOfErrorAndMessage_whenCallNewDomainException_ThenReturnDomainException() {
        var errors = List.of(new Error("Common Error"));

        var domainException = DomainException.with("Common Error", errors);

        Assertions.assertEquals(errors, domainException.getErrors());
    }

    @Test
    void givenAValidMessage_whenCallNewDomainException_ThenReturnDomainException() {
        var message = "Common Error";

        var domainException = DomainException.with(message);

        Assertions.assertEquals(message, domainException.getMessage());
    }
}
