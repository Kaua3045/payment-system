package com.payment.system.application.usecases.transactions.retrieve.id;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class GetTransactionByIdUseCase extends UseCase<GetTransactionByIdCommand, GetTransactionByIdOutput> {

    protected GetTransactionByIdUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
