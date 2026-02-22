package com.payment.system.application.usecases.accounts.create;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class CreateAccountUseCase extends UseCase<CreateAccountCommand, CreateAccountOutput> {

    protected CreateAccountUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
