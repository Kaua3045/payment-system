package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.create.DefaultCreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.DefaultListPixKeysUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysUseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.infrastructure.wrapper.Slf4jApplicationLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PixKeyUseCaseConfig {

    @Bean
    public CreatePixKeyUseCase createPixKeyUseCase(
            final PixKeyRepository pixKeyRepository,
            final AccountRepository accountRepository,
            final Metrics metrics
    ) {
        return new DefaultCreatePixKeyUseCase(
                pixKeyRepository,
                accountRepository,
                metrics,
                new Slf4jApplicationLogger(CreatePixKeyUseCase.class)
        );
    }

    @Bean
    public ListPixKeysUseCase listPixKeysUseCase(
            final PixKeyRepository pixKeyRepository
    ) {
        return new DefaultListPixKeysUseCase(pixKeyRepository, new Slf4jApplicationLogger(ListPixKeysUseCase.class));
    }
}
