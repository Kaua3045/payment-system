package com.kaua.template.infrastructure.wrapper;

import com.kaua.template.application.wrapper.ObservationContext;
import com.kaua.template.domain.utils.Generated;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.List;
import java.util.concurrent.Callable;

@Generated
public class ObservationContextOtel implements ObservationContext {

    private final Tracer tracer;
    private final Span parentSpan;

    public ObservationContextOtel(final Tracer tracer, final Span parentSpan) {
        this.tracer = tracer;
        this.parentSpan = parentSpan;
    }

    @Override
    public void setAttribute(final String key, final Object value) {
        switch (value) {
            case String s -> parentSpan.setAttribute(key, s);
            case Long l -> parentSpan.setAttribute(key, l);
            case Integer i -> parentSpan.setAttribute(key, i.longValue());
            case Double d -> parentSpan.setAttribute(key, d);
            case Boolean b -> parentSpan.setAttribute(key, b);
            case List<?> list -> handleListAttribute(parentSpan, key, list);
            case null -> parentSpan.setAttribute(key, null);
            default -> parentSpan.setAttribute(key, value.toString());
        }
    }

    @Override
    public void recordException(Throwable t) {
        parentSpan.recordException(t);
        parentSpan.setStatus(StatusCode.ERROR);
    }

    @Override
    public void addEvent(String name) {
        parentSpan.addEvent(name);
    }

    @Override
    public String traceId() {
        Span span = Span.current();
        if (span != null && span.getSpanContext().isValid()) {
            return span.getSpanContext().getTraceId();
        }
        if (parentSpan != null && parentSpan.getSpanContext().isValid()) {
            return parentSpan.getSpanContext().getTraceId();
        }
        return null;
    }

    @Override
    public <T> T runInSpanInternal(String name, Callable<T> block) {
        Span subSpan = tracer.spanBuilder(name)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope ignored = subSpan.makeCurrent()) {
            return block.call();
        } catch (Throwable ex) {
            subSpan.recordException(ex);
            subSpan.setStatus(StatusCode.ERROR);
            sneakyThrow(ex);
            return null;
        } finally {
            subSpan.end();
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }

    @SuppressWarnings("unchecked")
    private void handleListAttribute(final Span span, final String key, final List<?> list) {
        if (list.isEmpty()) return;

        Object first = list.getFirst();
        switch (first) {
            case String s -> span.setAttribute(AttributeKey.stringArrayKey(key), (List<String>) list);
            case Long l -> span.setAttribute(AttributeKey.longArrayKey(key), (List<Long>) list);
            case Integer i -> {
                List<Long> longs = ((List<Integer>) list).stream()
                        .map(Integer::longValue)
                        .toList();
                span.setAttribute(AttributeKey.longArrayKey(key), longs);
            }
            case Double d -> span.setAttribute(AttributeKey.doubleArrayKey(key), (List<Double>) list);
            case Boolean b -> span.setAttribute(AttributeKey.booleanArrayKey(key), (List<Boolean>) list);
            default -> {
                List<String> strings = list.stream()
                        .map(Object::toString)
                        .toList();
                span.setAttribute(AttributeKey.stringArrayKey(key), strings);
            }
        }
    }
}
