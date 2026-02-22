package com.payment.system.application.usecases.transactions.create;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class CreateTransactionUseCase extends UseCase<CreateTransactionCommand, CreateTransactionOutput> {

    protected CreateTransactionUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
