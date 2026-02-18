package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.utils.Generated;
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
    public void incrementCounter(String name, long value) {
        counters.computeIfAbsent(name, n ->
                meter.counterBuilder(n)
                        .build()
        ).add(value);
    }

    @Override
    public void recordTime(String name, long milliseconds) {
        histograms.computeIfAbsent(name, n ->
                meter.histogramBuilder(n)
                        .ofLongs()
                        .build()
        ).record(milliseconds);
    }

    @Override
    public void gauge(String name, long value) {
        gauges.computeIfAbsent(name, n -> {
            AtomicLong atomic = new AtomicLong();
            meter.gaugeBuilder(n)
                    .ofLongs()
                    .buildWithCallback(measurement ->
                            measurement.record(atomic.get())
                    );
            return atomic;
        }).set(value);
    }
}
