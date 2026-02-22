package com.payment.system.application;

import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class UnitUseCase<I> {

    protected final ApplicationLogger logger;

    protected UnitUseCase(final ApplicationLogger aLogger) {
        this.logger = aLogger;
    }

    public abstract void execute(I input);
}
