package com.payment.system.application;

import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class UseCase<I, O> {

    protected final ApplicationLogger logger;

    protected UseCase(final ApplicationLogger aLogger) {
        this.logger = aLogger;
    }

    public abstract O execute(I input);
}
