package com.payment.system.application.usecases.accounts.close;

import com.payment.system.application.UnitUseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class CloseAccountUseCase extends UnitUseCase<CloseAccountCommand> {

    protected CloseAccountUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
