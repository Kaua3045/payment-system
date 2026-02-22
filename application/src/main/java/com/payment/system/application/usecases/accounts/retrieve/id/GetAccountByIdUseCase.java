package com.payment.system.application.usecases.accounts.retrieve.id;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class GetAccountByIdUseCase extends UseCase<GetAccountByIdCommand, GetAccountByIdOutput> {

    protected GetAccountByIdUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
