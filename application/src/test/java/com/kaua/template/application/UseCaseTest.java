package com.kaua.template.application;

import com.kaua.template.application.wrapper.ObservationContext;
import com.kaua.template.application.wrapper.TracerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.quality.Strictness.LENIENT;

@Tag("unitTest")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public abstract class UseCaseTest {

    @Mock
    protected TracerWrapper tracerWrapper;

    @BeforeEach
    void setupTracerWrapper() {
        Mockito.when(tracerWrapper.traceWithReturn(Mockito.anyString(), Mockito.any()))
                .thenAnswer(invocation -> {
                    Function<ObservationContext, Object> block = invocation.getArgument(1);
                    return block.apply(new FakeObservationContext());
                });

        Mockito.doAnswer(invocation -> {
            Consumer<ObservationContext> block = invocation.getArgument(1);
            block.accept(new FakeObservationContext());
            return null;
        }).when(tracerWrapper).trace(Mockito.anyString(), Mockito.any());
    }
}
