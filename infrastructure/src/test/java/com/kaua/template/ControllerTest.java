package com.kaua.template;

import com.kaua.template.infrastructure.configurations.SecurityConfig;
import com.kaua.template.infrastructure.idempotency.gateways.InMemoryIdempotencyKeyGateway;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ActiveProfiles("test-integration")
@WebMvcTest
@Import({SecurityConfig.class, InMemoryIdempotencyKeyGateway.class, ObservationTest.OpenTelemetryTestConfig.class, IntegrationTestConfig.class})
@Tag("integrationTest")
public @interface ControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
