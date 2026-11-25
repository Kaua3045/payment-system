package com.payment.system.infrastructure.configurations;

import com.payment.system.application.gateways.FraudGateway;
import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.domain.frauds.rules.BlockList;
import com.payment.system.infrastructure.configurations.properties.FraudSettings;
import com.payment.system.infrastructure.frauds.blocklist.InMemoryBlockList;
import com.payment.system.infrastructure.frauds.blocklist.JdbcBlockListSource;
import com.payment.system.infrastructure.frauds.blocklist.RefreshingBlockList;
import com.payment.system.infrastructure.frauds.stats.InMemoryStatsRepository;
import com.payment.system.infrastructure.gateways.FraudGatewayImpl;
import com.payment.system.infrastructure.jdbc.DatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class FraudConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FraudConfiguration.class);

    @Bean
    @ConfigurationProperties("fraud")
    public FraudSettings fraudSettings() {
        return new FraudSettings();
    }

    @Bean
    public StatsRepository inMemoryStatsRepository() {
        return new InMemoryStatsRepository();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "fraud",
            name = "block-list-type",
            havingValue = "IN_MEMORY",
            matchIfMissing = true
    )
    public BlockList inMemoryBlockList() {
        return new InMemoryBlockList();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "fraud",
            name = "block-list-type",
            havingValue = "JDBC",
            matchIfMissing = true
    )
    public BlockList jdbcBlockList(
            DatabaseClient databaseClient,
            FraudSettings settings
    ) {
        return new RefreshingBlockList(
                new JdbcBlockListSource(databaseClient),
                Duration.ofSeconds(settings.getBlockListRefreshIntervalSeconds()),
                e -> logger.warn("Failed to refresh fraud blocklist", e)
        );
    }

    @Bean(destroyMethod = "close")
    public FraudModule fraudModule(FraudSettings settings, BlockList blockList, StatsRepository statsRepository, Metrics metrics, TracerWrapper tracer) throws IOException {
        return FraudModule.create(settings, blockList, statsRepository, metrics, tracer);
    }

    @Bean
    public FraudGateway fraudGateway(FraudModule module, FraudSettings settings) {
        return new FraudGatewayImpl(module, settings.getFailOpenBelow());
    }
}
