package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.FraudGateway;
import com.payment.system.application.usecases.frauds.assess.AssessFraudUseCase;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.RequestContext;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.configurations.FraudModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudGatewayImplTest extends UnitTest {

    @Mock
    private FraudModule fraudModule;

    @Mock
    private AssessFraudUseCase useCase;

    private FraudGatewayImpl fraudGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(fraudModule.getUseCase()).thenReturn(useCase);

        fraudGateway = new FraudGatewayImpl(
                fraudModule,
                new BigDecimal("100.00")
        );
    }

    @Test
    void shouldReturnAssessmentWhenFraudAssessmentSucceeds() {
        final var attempt = Attempt.pix(
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                "21787y91qwsy7uu197y",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("100.00")),
                InstantUtils.now(),
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("128.458.271.11")
        );
        final var expected = new Assessment(Verdict.APPROVE, 0.15, List.of(), false);

        when(useCase.execute(attempt)).thenReturn(expected);

        final var actual = fraudGateway.assess(new FraudGateway.AttemptRequest(
                attempt.id(),
                attempt.origin(),
                attempt.destination(),
                attempt.amount(),
                attempt.destinationCreatedAt(),
                attempt.details(),
                attempt.context()
        ));

        assertNotNull(actual);
        verify(fraudModule).getUseCase();
    }

    @Test
    void shouldReturnApproveFallbackWhenFraudAssessmentFailsAndAmountIsBelowThreshold() {
        final var attempt = Attempt.pix(
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                "21787y91qwsy7uu197y",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("100.00")),
                InstantUtils.now(),
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("128.458.271.11")
        );

        when(useCase.execute(attempt)).thenThrow(new RuntimeException("Fraud service failed"));

        final var actual = fraudGateway.assess(new FraudGateway.AttemptRequest(
                attempt.id(),
                attempt.origin(),
                attempt.destination(),
                attempt.amount(),
                attempt.destinationCreatedAt(),
                attempt.details(),
                attempt.context()
        ));


        assertEquals(Verdict.APPROVE, actual.verdict());
        assertFalse(actual.reasons().isEmpty());
    }

    @Test
    void shouldReturnManualReviewFallbackWhenFraudAssessmentFailsAndAmountIsAboveThreshold() {
        final var attempt = Attempt.pix(
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                IdentifierUtils.generateNewId(),
                "21787y91qwsy7uu197y",
                InstantUtils.now().minus(10, ChronoUnit.HOURS),
                Money.of(new BigDecimal("150.00")),
                InstantUtils.now(),
                InstantUtils.now().minus(10, ChronoUnit.DAYS),
                new RequestContext("128.458.271.11")
        );

        when(useCase.execute(attempt)).thenThrow(new RuntimeException("Fraud service failed"));

        final var actual = fraudGateway.assess(new FraudGateway.AttemptRequest(
                attempt.id(),
                attempt.origin(),
                attempt.destination(),
                attempt.amount(),
                attempt.destinationCreatedAt(),
                attempt.details(),
                attempt.context()
        ));

        assertEquals(Verdict.MANUAL_REVIEW, actual.verdict());
        assertFalse(actual.reasons().isEmpty());
    }

}
