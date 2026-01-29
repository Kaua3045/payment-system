package com.kaua.template.application.wrapper;

import java.util.concurrent.Callable;

public interface ObservationContext {

    void setAttribute(String key, Object value);

    void recordException(Throwable t);

    void addEvent(String name);

    String traceId();

    default void runInSpan(final String name, Runnable block) {
        runInSpanInternal(name, () -> {
            block.run();
            return null;
        });
    }

    default <T> T runInSpan(final String name, Callable<T> block) {
        return runInSpanInternal(name, block);
    }

    default <T> T runInSpanInternal(final String name, Callable<T> block) {
        throw new UnsupportedOperationException("Method should be overridden by implementation");
    }
}
