package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.usecases.accounts.create.CreateAccountUseCase;
import com.payment.system.application.usecases.accounts.create.DefaultCreateAccountUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.DefaultGetAccountByIdUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdUseCase;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.infrastructure.wrapper.Slf4jApplicationLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AccountUseCaseConfig {

    @Bean
    public CreateAccountUseCase createAccountUseCase(
            final AccountRepository accountRepository,
            final Metrics metrics
    ) {
        return new DefaultCreateAccountUseCase(accountRepository, metrics, new Slf4jApplicationLogger(CreateAccountUseCase.class));
    }

    @Bean
    public GetAccountByIdUseCase getAccountByIdUseCase(
            final AccountRepository accountRepository
    ) {
        return new DefaultGetAccountByIdUseCase(accountRepository, new Slf4jApplicationLogger(GetAccountByIdUseCase.class));
    }
}
