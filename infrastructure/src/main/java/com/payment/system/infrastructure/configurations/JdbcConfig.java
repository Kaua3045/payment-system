package com.payment.system.infrastructure.configurations;

import com.payment.system.infrastructure.jdbc.DatabaseClient;
import com.payment.system.infrastructure.jdbc.JdbcClientAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
public class JdbcConfig {

    @Bean
    public DatabaseClient databaseClient(final JdbcClient jdbcClient, final NamedParameterJdbcOperations namedParameterJdbcOperations) {
        return new JdbcClientAdapter(jdbcClient, namedParameterJdbcOperations);
    }
}
