package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.create.DefaultCreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.DefaultListPixKeysUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysUseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PixKeyUseCaseConfig {

    @Bean
    public CreatePixKeyUseCase createPixKeyUseCase(
            final PixKeyRepository pixKeyRepository,
            final AccountRepository accountRepository,
            final Metrics metrics,
            final ApplicationLogger logger
    ) {
        return new DefaultCreatePixKeyUseCase(
                pixKeyRepository,
                accountRepository,
                metrics,
                logger
        );
    }

    @Bean
    public ListPixKeysUseCase listPixKeysUseCase(
            final PixKeyRepository pixKeyRepository,
            final ApplicationLogger logger
    ) {
        return new DefaultListPixKeysUseCase(pixKeyRepository, logger);
    }
}
