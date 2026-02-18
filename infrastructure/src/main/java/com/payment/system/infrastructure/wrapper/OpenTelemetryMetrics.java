package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.utils.Generated;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Generated
public class OpenTelemetryMetrics implements Metrics {

    private final Meter meter;

    public OpenTelemetryMetrics(final Meter meter) {
        this.meter = Objects.requireNonNull(meter);
    }

    @Override
    public void incrementCounter(final String name, final long value) {
        meter.counterBuilder(name)
                .build()
                .add(value);
    }

    @Override
    public void recordTime(final String name, final long milliseconds) {
        meter.histogramBuilder(name)
                .ofLongs()
                .build()
                .record(milliseconds);
    }

    @Override
    public void gauge(final String name, final long value) {
        meter.gaugeBuilder(name)
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(value));
    }
}
