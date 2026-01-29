package com.kaua.template.application.wrapper;

import com.kaua.template.application.UseCaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

class ObservationContextTest extends UseCaseTest {

    @Test
    void runInSpanInternal_default_throwsUnsupportedOperationException() {
        ObservationContext ctx = new ObservationContext() {
            @Override
            public void setAttribute(String key, Object value) {

            }

            @Override
            public void recordException(Throwable t) {

            }

            @Override
            public void addEvent(String name) {

            }

            @Override
            public String traceId() {
                return "";
            }
        };

        UnsupportedOperationException exception = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> ctx.runInSpanInternal("spanName", () -> null)
        );

        Assertions.assertEquals("Method should be overridden by implementation", exception.getMessage());
    }

    @Test
    void runInSpan_runnable_callsRunInSpanInternal_andExecutesBlock() {
        final boolean[] executed = {false};

        ObservationContext ctx = new ObservationContext() {
            @Override
            public void setAttribute(String key, Object value) {
            }

            @Override
            public void recordException(Throwable t) {
            }

            @Override
            public void addEvent(String name) {
            }

            @Override
            public String traceId() {
                return "";
            }

            @Override
            public <T> T runInSpanInternal(String name, Callable<T> block) {
                try {
                    return block.call();
                } catch (Exception e) {
                    sneakyThrow(e);
                    return null;
                }
            }
        };

        ctx.runInSpan("testSpan", () -> executed[0] = true);

        Assertions.assertTrue(executed[0], "Runnable block should be executed");
    }

    @Test
    void runInSpan_callable_callsRunInSpanInternal_andReturnsValue() {
        ObservationContext ctx = new ObservationContext() {
            @Override
            public void setAttribute(String key, Object value) {

            }

            @Override
            public void recordException(Throwable t) {

            }

            @Override
            public void addEvent(String name) {

            }

            @Override
            public String traceId() {
                return "";
            }

            @Override
            public <T> T runInSpanInternal(String name, Callable<T> block) {
                try {
                    return block.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        String result = ctx.runInSpan("testSpan", () -> "hello");

        Assertions.assertEquals("hello", result);
    }

    @Test
    void runInSpan_callable_propagatesException() {
        ObservationContext ctx = new ObservationContext() {
            @Override
            public void setAttribute(String key, Object value) {

            }

            @Override
            public void recordException(Throwable t) {

            }

            @Override
            public void addEvent(String name) {

            }

            @Override
            public String traceId() {
                return "";
            }

            @Override
            public <T> T runInSpanInternal(String name, Callable<T> block) {
                try {
                    return block.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> ctx.runInSpan("testSpan", () -> {
                    throw new Exception("fail");
                }));

        Assertions.assertEquals("fail", exception.getCause().getMessage());
    }

    @Test
    void addEvent_string_doesNotThrowException() {
        ObservationContext ctx = new ObservationContext() {
            @Override
            public void setAttribute(String key, Object value) {
            }

            @Override
            public void recordException(Throwable t) {
            }

            @Override
            public void addEvent(String name) {
                Assertions.assertEquals("test-event", name);
            }

            @Override
            public String traceId() {
                return "";
            }
        };

        Assertions.assertDoesNotThrow(() -> ctx.addEvent("test-event"));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }
}
