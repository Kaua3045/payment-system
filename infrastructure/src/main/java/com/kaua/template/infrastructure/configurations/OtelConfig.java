package com.kaua.template.infrastructure.configurations;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
public class OtelConfig {

    @Bean
    @ConditionalOnProperty(value = "application.otel.memory-exporter", havingValue = "true")
    public OpenTelemetrySdk openTelemetrySdk() {
        final var aInMemorySpanExporter = InMemorySpanExporter.create();

        final var aSdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(aInMemorySpanExporter))
                .addResource(Resource.getDefault()
                        .merge(Resource.create(Attributes.builder().put("service.name", "events-platform").build())))
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(aSdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    @Profile({"prod", "development", "sandbox-local"})
    public Tracer tracer(final BuildProperties buildProperties) {
        return GlobalOpenTelemetry.getTracer(buildProperties.getName());
    }
}
