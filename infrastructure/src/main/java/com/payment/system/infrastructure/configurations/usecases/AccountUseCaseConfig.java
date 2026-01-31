package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.usecases.accounts.create.CreateAccountUseCase;
import com.payment.system.application.usecases.accounts.create.DefaultCreateAccountUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AccountUseCaseConfig {

    @Bean
    public CreateAccountUseCase createAccountUseCase(
            final AccountRepository accountRepository
    ) {
        return new DefaultCreateAccountUseCase(accountRepository);
    }
}
