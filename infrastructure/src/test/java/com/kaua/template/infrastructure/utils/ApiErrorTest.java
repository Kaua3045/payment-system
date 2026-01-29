package com.kaua.template.infrastructure.utils;

import com.kaua.template.domain.UnitTest;
import com.kaua.template.domain.exceptions.DomainException;
import com.kaua.template.domain.utils.InstantUtils;
import com.kaua.template.domain.validation.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ApiErrorTest extends UnitTest {

    @Test
    void givenAValidDomainException_whenCallFrom_shouldReturnMessageAndErrorList() {
        final var aErrors = List.of(new Error("Error 1"), new Error("Error 2"));
        final var aDomainException = DomainException.with(aErrors);
        final var aNow = InstantUtils.now();

        final var aResult = ApiError.from(aDomainException, aNow);

        Assertions.assertEquals(aDomainException.getMessage(), aResult.message());
        Assertions.assertEquals(aErrors, aResult.errors());
        Assertions.assertEquals(aNow, aResult.timestamp());
    }

    @Test
    void givenAValidMessage_whenCallFrom_shouldReturnMessageAndEmptyErrorList() {
        final var aMessage = "Internal Server Error";
        final var aNow = InstantUtils.now();

        final var aResult = ApiError.from(aMessage, aNow);

        Assertions.assertEquals(aMessage, aResult.message());
        Assertions.assertEquals(0, aResult.errors().size());
        Assertions.assertEquals(aNow, aResult.timestamp());
    }
}
