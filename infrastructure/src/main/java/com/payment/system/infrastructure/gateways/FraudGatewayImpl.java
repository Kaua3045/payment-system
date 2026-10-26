package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.FraudGateway;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.infrastructure.configurations.FraudModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FraudGatewayImpl implements FraudGateway {

    private static final Logger log = LoggerFactory.getLogger(FraudGatewayImpl.class);

    private final FraudModule fraudModule;

    public FraudGatewayImpl(final FraudModule fraudModule) {
        this.fraudModule = fraudModule;
    }

    @Override
    public Assessment assess(final Attempt attempt) {
        log.info("Calling fraud service {}", attempt);
        final var assessment = this.fraudModule.getUseCase().execute(attempt);
        log.info("Fraud result with verdict {} score {} and reasons {}", assessment.verdict().name(), assessment.score(), assessment.reasons());
        return assessment;
    }
}
