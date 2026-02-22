package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.create.DefaultCreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositUseCase;
import com.payment.system.application.usecases.transactions.deposit.DefaultCreateDepositUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.DefaultGetTransactionByIdUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdUseCase;
import com.payment.system.application.usecases.transactions.retrieve.list.DefaultListTransactionsUseCase;
import com.payment.system.application.usecases.transactions.retrieve.list.ListTransactionsUseCase;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.infrastructure.wrapper.Slf4jApplicationLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TransactionUseCaseConfig {

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(
            final AccountRepository accountRepository,
            final PixKeyRepository pixKeyRepository,
            final TransactionRepository transactionRepository,
            final TransactionManager transactionManager,
            final Metrics metrics
    ) {
        return new DefaultCreateTransactionUseCase(
                accountRepository,
                pixKeyRepository,
                transactionRepository,
                transactionManager,
                metrics,
                new Slf4jApplicationLogger(CreateTransactionUseCase.class)
        );
    }

    @Bean
    public GetTransactionByIdUseCase getTransactionByIdUseCase(
            final TransactionRepository transactionRepository
    ) {
        return new DefaultGetTransactionByIdUseCase(
                transactionRepository,
                new Slf4jApplicationLogger(GetTransactionByIdUseCase.class)
        );
    }

    @Bean
    public CreateDepositUseCase createDepositUseCase(
            final AccountRepository accountRepository,
            final PixKeyRepository pixKeyRepository,
            final TransactionRepository transactionRepository,
            final TransactionManager transactionManager,
            final Metrics metrics
    ) {
        return new DefaultCreateDepositUseCase(
                accountRepository,
                pixKeyRepository,
                transactionRepository,
                transactionManager,
                metrics,
                new Slf4jApplicationLogger(CreateDepositUseCase.class)
        );
    }

    @Bean
    public ListTransactionsUseCase listTransactionsUseCase(
            final TransactionRepository transactionRepository
    ) {
        return new DefaultListTransactionsUseCase(
                transactionRepository,
                new Slf4jApplicationLogger(ListTransactionsUseCase.class)
        );
    }
}
