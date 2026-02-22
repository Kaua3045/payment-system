package com.payment.system.application.wrapper;

public interface ApplicationLogger {

    void info(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);
}
