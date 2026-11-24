package com.payment.system.application.gateways;

import com.payment.system.domain.frauds.*;
import com.payment.system.domain.valueobjects.Money;

import java.time.Instant;
import java.util.List;

public interface FraudGateway {

    AssessmentResponse assess(AttemptRequest attempt);

    record AttemptRequest(
            String idempotencyKey, // Idempotency key used in request attempts
            String origin,
            String destination,
            Money amount,
            Instant destinationCreatedAt,
            ChannelDetails details,
            RequestContext context
    ) {}

    record AssessmentResponse(
            Verdict verdict,
            double score,
            List<String> reasons,
            boolean degraded
    ) {}
}
