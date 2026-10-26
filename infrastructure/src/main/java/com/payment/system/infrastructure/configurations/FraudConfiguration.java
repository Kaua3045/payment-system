package com.payment.system.infrastructure.configurations;

import com.payment.system.application.repositories.StatsRepository;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.domain.frauds.rules.BlockList;
import com.payment.system.infrastructure.configurations.properties.FraudSettings;
import com.payment.system.infrastructure.frauds.blocklist.InMemoryBlockList;
import com.payment.system.infrastructure.frauds.stats.InMemoryStatsRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FraudConfiguration {

    @Bean
    @ConfigurationProperties("fraud")
    public FraudSettings fraudSettings() {
        return new FraudSettings();
    }

    @Bean
    public StatsRepository inMemoryStatsRepository() {
        return new InMemoryStatsRepository();
    }

    // TODO in future read path to in memory
    @Bean
    public BlockList fraudBlocklist() {
        return new InMemoryBlockList();
    }

    @Bean
    public FraudModule fraudModule(FraudSettings settings, BlockList blockList, StatsRepository statsRepository, Metrics metrics, TracerWrapper tracer) throws IOException {
        return FraudModule.create(settings, blockList, statsRepository, metrics, tracer);
    }
}
