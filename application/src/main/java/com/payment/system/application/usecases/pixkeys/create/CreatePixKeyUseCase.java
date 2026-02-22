package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class CreatePixKeyUseCase extends UseCase<CreatePixKeyCommand, CreatePixKeyOutput> {

    protected CreatePixKeyUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
