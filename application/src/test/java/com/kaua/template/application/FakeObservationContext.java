package com.kaua.template.application;

import com.kaua.template.application.wrapper.ObservationContext;
import com.kaua.template.domain.utils.IdentifierUtils;

import java.util.concurrent.Callable;

public class FakeObservationContext implements ObservationContext {

    @Override
    public void setAttribute(String key, Object value) {
        // no-op
    }

    @Override
    public void recordException(Throwable t) {
        // no-op
    }

    @Override
    public void addEvent(String name) {
        // no-op
    }

    @Override
    public String traceId() {
        return IdentifierUtils.generateNewId();
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

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }
}
