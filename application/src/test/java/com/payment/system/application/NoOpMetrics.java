package com.payment.system.application;

import com.payment.system.application.wrapper.Metrics;

import java.util.Map;

public class NoOpMetrics implements Metrics {

    @Override
    public void incrementCounter(String name, long value, Map<String, String> attributes) {

    }

    @Override
    public void recordTime(String name, long milliseconds, Map<String, String> attributes) {

    }

    @Override
    public void gauge(String name, long value, Map<String, String> attributes) {

    }
}
