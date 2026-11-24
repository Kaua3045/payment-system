package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.FraudGateway;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.utils.InstantUtils;
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
    public AssessmentResponse assess(final AttemptRequest attempt) {
        try {
            log.info("Calling fraud service {}", attempt);
            final var assessment = this.fraudModule.getUseCase().execute(new Attempt(
                    attempt.idempotencyKey(),
                    attempt.origin(),
                    attempt.destination(),
                    attempt.amount(),
                    InstantUtils.now(),
                    null,
                    attempt.destinationCreatedAt(),
                    attempt.details(),
                    attempt.context()
            ));
            log.info("Fraud result with verdict {} score {} and reasons {}", assessment.verdict(), assessment.score(), assessment.reasons());
            return new AssessmentResponse(assessment.verdict(), assessment.score(), assessment.reasons(), assessment.degraded());
        } catch (RuntimeException ex) {
            Verdict v = attempt.amount().amount().compareTo(failOpenBelow) <= 0 ? Verdict.APPROVE : Verdict.MANUAL_REVIEW;
            final var assessment = Assessment.fallback(v, ex.getClass().getSimpleName());
            return new AssessmentResponse(assessment.verdict(), assessment.score(), assessment.reasons(), assessment.degraded());
        }
    }
}
