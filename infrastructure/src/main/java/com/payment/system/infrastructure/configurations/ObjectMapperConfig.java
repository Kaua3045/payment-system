package com.payment.system.infrastructure.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.system.infrastructure.configurations.json.Json;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;

@JsonComponent
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper mapper() {
        return Json.mapper();
    }
}
