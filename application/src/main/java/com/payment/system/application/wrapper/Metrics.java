package com.payment.system.application.wrapper;

public interface Metrics {

    void incrementCounter(String name, long value);

    void recordTime(String name, long milliseconds);

    void gauge(String name, long value);
}
