package com.payment.system.application.usecases.pixkeys.retrieve.get;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;

public abstract class GetPixKeyByValueUseCase extends UseCase<GetPixKeyByValueCommand, GetPixKeyByValueOutput> {

    protected GetPixKeyByValueUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
