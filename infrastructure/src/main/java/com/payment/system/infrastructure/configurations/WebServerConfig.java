package com.payment.system.infrastructure.configurations;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.payment.system")
public class WebServerConfig {
}
