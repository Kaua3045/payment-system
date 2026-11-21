package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.FraudGateway;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.infrastructure.configurations.FraudModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

public class FraudGatewayImpl implements FraudGateway {

    private static final Logger log = LoggerFactory.getLogger(FraudGatewayImpl.class);

    private final FraudModule fraudModule;
    private final BigDecimal failOpenBelow;

    public FraudGatewayImpl(final FraudModule fraudModule, final BigDecimal failOpenBelow) {
        this.fraudModule = fraudModule;
        this.failOpenBelow = failOpenBelow;
    }

    @Override
    public Assessment assess(final Attempt attempt) {
        try {
            log.info("Calling fraud service {}", attempt);
            final var assessment = this.fraudModule.getUseCase().execute(attempt);
            log.info("Fraud result with verdict {} score {} and reasons {}", assessment.verdict(), assessment.score(), assessment.reasons());
            return assessment;
        } catch (RuntimeException ex) {
            Verdict v = attempt.amount().amount().compareTo(failOpenBelow) <= 0 ? Verdict.APPROVE : Verdict.MANUAL_REVIEW;
            return Assessment.fallback(v, ex.getClass().getSimpleName());
        }
    }
}
