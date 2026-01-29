package com.payment.system;

import com.payment.system.domain.utils.InstantUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    @Bean
    public BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "payment-system");
        properties.setProperty("version", "0.0.1");
        properties.setProperty("time", InstantUtils.now().toString());
        return new BuildProperties(properties);
    }
}
