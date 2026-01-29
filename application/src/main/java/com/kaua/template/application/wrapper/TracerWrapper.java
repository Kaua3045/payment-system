package com.kaua.template.application.wrapper;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TracerWrapper {

    <T> T traceWithReturn(String spanName, Function<ObservationContext, T> block);

    void trace(String spanName, Consumer<ObservationContext> block);
}

