package com.payment.system.domain.exceptions;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoStackTraceExceptionTest extends UnitTest {

    @Test
    void testCallNoStacktraceExceptionWithoutCause() {
        final var exception = new NoStackTraceException("message", "no_stack_trace");

        Assertions.assertEquals("message", exception.getMessage());
        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals("no_stack_trace", exception.getErrorCode());
    }

    @Test
    void testCallNoStacktraceExceptionWithCause() {
        final var cause = new RuntimeException();
        final var exception = new NoStackTraceException("message", cause, "no_stack_trace");

        Assertions.assertEquals("message", exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals("no_stack_trace", exception.getErrorCode());
    }
}
