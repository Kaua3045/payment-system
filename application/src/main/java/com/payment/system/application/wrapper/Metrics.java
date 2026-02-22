package com.payment.system.application.wrapper;

import java.util.Map;

public interface Metrics {

    void incrementCounter(String name, long value, Map<String, String> attributes);

    void recordTime(String name, long milliseconds, Map<String, String> attributes);

    void gauge(String name, long value, Map<String, String> attributes);
}
