package com.payment.system.infrastructure.gateways;

import com.payment.system.application.usecases.frauds.assess.AssessFraudUseCase;
import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.infrastructure.configurations.FraudModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FraudGatewayImplTest extends UnitTest {

    @Mock
    private FraudModule fraudModule;

    @Mock
    private AssessFraudUseCase useCase;

    @InjectMocks
    private FraudGatewayImpl fraudGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(fraudModule.getUseCase()).thenReturn(useCase);
    }

    @Test
    void shouldReturnAssessmentWhenFraudAssessmentSucceeds() {
        final var attempt = mock(Attempt.class);
        final var expected = mock(Assessment.class);

        when(useCase.execute(attempt)).thenReturn(expected);

        final var actual = fraudGateway.assess(attempt);

        assertSame(expected, actual);
        verify(fraudModule).getUseCase();
        verify(useCase).execute(attempt);
    }

    @Test
    void shouldPropagateExceptionWhenFraudAssessmentFails() {
        final var attempt = mock(Attempt.class);
        final var exception = new RuntimeException("Fraud service failed");

        when(useCase.execute(attempt)).thenThrow(exception);

        final var actual = assertThrows(
                RuntimeException.class,
                () -> fraudGateway.assess(attempt)
        );

        assertSame(exception, actual);
        verify(useCase).execute(attempt);
    }
}
