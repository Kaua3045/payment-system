package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.utils.Generated;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Generated
public class OpenTelemetryMetrics implements Metrics {

    private final Meter meter;
    private final Map<String, LongCounter> counters = new ConcurrentHashMap<>();
    private final Map<String, LongHistogram> histograms = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    public OpenTelemetryMetrics(final Meter meter) {
        this.meter = meter;
    }

    @Override
    public void incrementCounter(String name, long value, Map<String, String> attributes) {

        LongCounter counter = counters.computeIfAbsent(name, n ->
                meter.counterBuilder(n)
                        .build()
        );

        counter.add(value, buildAttributes(attributes));
    }

    @Override
    public void recordTime(String name, long milliseconds, Map<String, String> attributes) {

        LongHistogram histogram = histograms.computeIfAbsent(name, n ->
                meter.histogramBuilder(n)
                        .ofLongs()
                        .build()
        );

        histogram.record(milliseconds, buildAttributes(attributes));
    }

    @Override
    public void gauge(String name, long value, Map<String, String> attributes) {

        AtomicLong atomic = gauges.computeIfAbsent(name, n -> {
            AtomicLong newAtomic = new AtomicLong();

            meter.gaugeBuilder(n)
                    .ofLongs()
                    .buildWithCallback(measurement ->
                            measurement.record(newAtomic.get(), buildAttributes(attributes))
                    );

            return newAtomic;
        });

        atomic.set(value);
    }

    private Attributes buildAttributes(Map<String, String> attributes) {

        AttributesBuilder builder = Attributes.builder();

        if (attributes != null) {
            attributes.forEach((k, v) ->
                    builder.put(AttributeKey.stringKey(k), v)
            );
        }

        return builder.build();
    }
}
