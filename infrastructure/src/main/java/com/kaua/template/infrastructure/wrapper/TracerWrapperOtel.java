package com.kaua.template.infrastructure.wrapper;

import com.kaua.template.application.wrapper.ObservationContext;
import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.domain.utils.Generated;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Generated
@Component
public class TracerWrapperOtel implements TracerWrapper {

    private final Tracer tracer;

    public TracerWrapperOtel(final Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer);
    }

    @Override
    public <T> T traceWithReturn(final String spanName, final Function<ObservationContext, T> block) {
        Span span = tracer.spanBuilder(spanName)
                .setParent(Context.current()).startSpan();

        try (Scope scope = span.makeCurrent()) {
            return block.apply(new ObservationContextOtel(tracer, span));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void trace(final String spanName, final Consumer<ObservationContext> block) {
        Span span = tracer.spanBuilder(spanName)
                .setParent(Context.current()).startSpan();

        try (Scope scope = span.makeCurrent()) {
            block.accept(new ObservationContextOtel(tracer, span));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
