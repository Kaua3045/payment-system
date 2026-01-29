package com.kaua.template;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

public interface ObservationTest {

    InMemorySpanExporter getSpanExporter();

    default void resetSpans() {
        getSpanExporter().reset();
    }

    default List<SpanData> getExportedSpans() {
        return getSpanExporter().getFinishedSpanItems();
    }

    default void assertSpanCreated(String expectedName) {
        List<SpanData> spans = getExportedSpans();
        boolean found = spans.stream().anyMatch(span -> span.getName().equals(expectedName));
        if (!found) {
            throw new AssertionError("Expected span '" + expectedName + "' not found. Found: " + spans);
        }
    }

    default void assertSpanAttribute(String spanName, String key, Object value) {
        List<SpanData> spans = getExportedSpans();
        final var aSpan = spans.stream()
                .filter(span -> span.getName().equals(spanName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Span not found: " + spanName));

        AttributeKey<?> attributeKey = getAttributeKey(key, value);
        Object attributeValue = aSpan.getAttributes().get(attributeKey);

        if (attributeValue == null) {
            throw new AssertionError("Attribute not found: " + key);
        }

        if (!attributeValue.equals(value)) {
            throw new AssertionError("Attribute value mismatch: " + key + " - " + value);
        }
    }

    private AttributeKey<?> getAttributeKey(String key, Object value) {
        return switch (value) {
            case String s -> AttributeKey.stringKey(key);
            case Long l -> AttributeKey.longKey(key);
            case Boolean b -> AttributeKey.booleanKey(key);
            case Double v -> AttributeKey.doubleKey(key);
            case null, default -> throw new AssertionError("Unsupported value type: " + value.getClass());
        };
    }

    @TestConfiguration
    class OpenTelemetryTestConfig {

        private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

        @Bean
        public InMemorySpanExporter spanExporter() {
            return spanExporter;
        }

        @Bean
        public OpenTelemetry openTelemetry() {
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build();

            return OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
        }

        @Bean
        public Tracer tracer() {
            return openTelemetry().getTracer("tests");
        }
    }
}
