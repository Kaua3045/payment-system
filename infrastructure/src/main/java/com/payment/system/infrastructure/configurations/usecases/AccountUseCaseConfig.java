package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.usecases.accounts.create.CreateAccountUseCase;
import com.payment.system.application.usecases.accounts.create.DefaultCreateAccountUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.DefaultGetAccountByIdUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdUseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AccountUseCaseConfig {

    @Bean
    public CreateAccountUseCase createAccountUseCase(
            final AccountRepository accountRepository,
            final Metrics metrics,
            final ApplicationLogger logger
    ) {
        return new DefaultCreateAccountUseCase(accountRepository, metrics, logger);
    }

    @Bean
    public GetAccountByIdUseCase getAccountByIdUseCase(
            final AccountRepository accountRepository,
            final ApplicationLogger logger
    ) {
        return new DefaultGetAccountByIdUseCase(accountRepository, logger);
    }
}
