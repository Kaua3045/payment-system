package com.payment.system.application.usecases.frauds.assess;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;

public abstract class AssessFraudUseCase extends UseCase<Attempt, Assessment> {

    protected AssessFraudUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
