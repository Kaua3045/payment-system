package com.kaua.template;

import com.kaua.template.infrastructure.configurations.WebServerConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ActiveProfiles("test-integration")
@SpringBootTest(classes = {
        WebServerConfig.class,
        IntegrationTestConfig.class,
        ObservationTest.OpenTelemetryTestConfig.class
})
@Tag("integrationTest")
public @interface IntegrationTest {
}
