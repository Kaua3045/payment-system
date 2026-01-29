package com.kaua.template.domain.validation;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ValidationHandlerTest extends UnitTest {

    @Test
    void givenAErrors_whenCallHasError_thenShouldReturnTrue() {
        final ValidationHandler handler = new TestValidationHandler();
        handler.append(new Error("error1"));
        handler.append(new Error("error2"));

        final boolean hasErrors = handler.hasError();

        Assertions.assertTrue(hasErrors);
    }

    @Test
    void givenNoErrors_whenCallHasError_thenShouldReturnFalse() {
        final ValidationHandler handler = new TestValidationHandler();

        final boolean hasErrors = handler.hasError();

        Assertions.assertFalse(hasErrors);
    }

    @Test
    void givenANullGetErrors_whenCallHasError_thenShouldReturnFalse() {
        final ValidationHandler handler = new TestValidationHandler() {
            @Override
            public List<Error> getErrors() {
                return null;
            }
        };

        final boolean hasErrors = handler.hasError();

        Assertions.assertFalse(hasErrors);
    }

    @Test
    void givenAError_whenCallFirstError_thenShouldReturnTheFirstError() {
        final ValidationHandler handler = new TestValidationHandler();
        handler.append(new Error("error1"));
        handler.append(new Error("error2"));

        final Error firstError = handler.firstError();

        Assertions.assertEquals("error1", firstError.message());
    }

    @Test
    void givenNoErrors_whenCallFirstError_thenShouldReturnNull() {
        final ValidationHandler handler = new TestValidationHandler();

        final Error firstError = handler.firstError();

        Assertions.assertNull(firstError);
    }

    @Test
    void givenANullGetErrors_whenCallFirstError_thenShouldReturnNull() {
        final ValidationHandler handler = new TestValidationHandler() {
            @Override
            public List<Error> getErrors() {
                return null;
            }
        };

        final Error firstError = handler.firstError();

        Assertions.assertNull(firstError);
    }

    static class TestValidationHandler implements ValidationHandler {

        private final List<Error> errors;

        public TestValidationHandler() {
            this.errors = new ArrayList<>();
        }

        @Override
        public ValidationHandler append(Error anError) {
            errors.add(anError);
            return this;
        }

        @Override
        public ValidationHandler append(ValidationHandler aHandler) {
            errors.addAll(aHandler.getErrors());
            return this;
        }

        @Override
        public <T> T validate(final Validation<T> aValidation) {
            aValidation.validate();
            return null;
        }


        @Override
        public List<Error> getErrors() {
            return errors;
        }
    }
}
