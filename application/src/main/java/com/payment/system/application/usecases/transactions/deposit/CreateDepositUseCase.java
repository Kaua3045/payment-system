package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class CreateDepositUseCase extends UseCase<CreateDepositCommand, CreateDepositOutput> {

    protected CreateDepositUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
