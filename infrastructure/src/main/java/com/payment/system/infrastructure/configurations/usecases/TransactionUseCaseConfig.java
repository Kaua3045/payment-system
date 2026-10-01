package com.payment.system.infrastructure.configurations.usecases;

import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.*;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.create.DefaultCreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositUseCase;
import com.payment.system.application.usecases.transactions.deposit.DefaultCreateDepositUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.DefaultGetTransactionByIdUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdUseCase;
import com.payment.system.application.usecases.transactions.retrieve.list.DefaultListTransactionsUseCase;
import com.payment.system.application.usecases.transactions.retrieve.list.ListTransactionsUseCase;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.infrastructure.wrapper.Slf4jApplicationLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TransactionUseCaseConfig {

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(
            final AccountGateway accountGateway,
            final PixKeyGateway pixKeyGateway,
            final TransactionRepository transactionRepository,
            final LedgerRepository ledgerRepository,
            final TransactionManager transactionManager,
            final Metrics metrics,
            final TracerWrapper tracerWrapper
            ) {
        return new DefaultCreateTransactionUseCase(
                accountGateway,
                pixKeyGateway,
                transactionRepository,
                ledgerRepository,
                transactionManager,
                metrics,
                new Slf4jApplicationLogger(CreateTransactionUseCase.class),
                tracerWrapper
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
            final AccountGateway accountGateway,
            final PixKeyGateway pixKeyGateway,
            final TransactionRepository transactionRepository,
            final LedgerRepository ledgerRepository,
            final TransactionManager transactionManager,
            final Metrics metrics
    ) {
        return new DefaultCreateDepositUseCase(
                accountGateway,
                pixKeyGateway,
                transactionRepository,
                ledgerRepository,
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
