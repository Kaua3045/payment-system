package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.Generated;
import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class DefaultCreateDepositUseCase extends CreateDepositUseCase {

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionManager transactionManager;
    private final Metrics metrics;

    public DefaultCreateDepositUseCase(
            final AccountRepository accountRepository,
            final PixKeyRepository pixKeyRepository,
            final TransactionRepository transactionRepository,
            final TransactionManager transactionManager,
            final Metrics metrics
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public CreateDepositOutput execute(final CreateDepositCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateDepositUseCase.class);
        }

        final var aStartTime = System.currentTimeMillis();

        try {
            this.metrics.incrementCounter("deposits.requested", 1);
            return this.transactionManager.execute(() -> {
                if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw DomainException.with("Amount must be greater than zero");
                }

                final var aSource = DepositSource.from(input.source())
                        .orElseThrow(() -> NotFoundException.with("DepositSource %s not found".formatted(input.source())));

                final var aPixKeyType = PixKeyType.from(input.pixKeyType())
                        .orElseThrow(() -> NotFoundException.with("PixKeyType %s not found".formatted(input.pixKeyType())));

                final var aKey = new PixKeyValueFactory().create(aPixKeyType, input.pixKey());

                final var aPixKey = this.pixKeyRepository.pixKeyOfActiveByValue(aKey.value())
                        .orElseThrow(NotFoundException.with(PixKey.class, "value", input.pixKey()));

                final var aToAccount = this.accountRepository.accountOfId(aPixKey.getAccountId().value().toString())
                        .orElseThrow(NotFoundException.with(Account.class, aPixKey.getAccountId().value().toString()));

                if (!aToAccount.getStatus().equals(AccountStatus.ACTIVE)) {
                    throw DomainException.with("To account is not active");
                }

                final var aTransaction = Transaction.newTransaction(
                        AccountId.system(),
                        aToAccount.getId(),
                        aPixKey.getId(),
                        new Money(input.amount()),
                        TransactionType.TRANSFER,
                        aSource,
                        input.idempotencyKey()
                );

                this.transactionRepository.save(aTransaction);

                aToAccount.credit(input.amount());

                this.accountRepository.save(aToAccount);

                aTransaction.complete();
                this.transactionRepository.save(aTransaction);

                this.metrics.incrementCounter("deposits.processed", 1);
                return CreateDepositOutput.from(aTransaction);
            });
        } catch (final DomainException ex) {
            this.transactionManager.execute(() -> {
                this.transactionRepository.transactionOfIdempotencyKey(input.idempotencyKey())
                        .ifPresent(tx -> {
                            tx.fail(ex.getMessage());
                            this.transactionRepository.save(tx);
                        });
                return null;
            });

            this.metrics.incrementCounter("deposits.failed", 1);
            this.metrics.incrementCounter(resolveErrorMetric(ex), 1);

            throw ex;
        } finally {
            final var aDuration = System.currentTimeMillis() - aStartTime;
            this.metrics.incrementCounter("deposits.latency", aDuration);
        }
    }

    @Generated
    private String resolveErrorMetric(final Exception ex) {
        if (ex instanceof NotFoundException notFound) {
            final var aMessage = notFound.getMessage().toLowerCase();

            if (aMessage.contains("account")) {
                return "deposits.error.account_not_found";
            }

            if (aMessage.contains("pixkey")) {
                return "deposits.error.pixkey_not_found";
            }

            return "deposits.error.not_found";
        }

        if (ex instanceof DomainException domain) {
            final var aMessage = domain.getMessage().toLowerCase();

            if (aMessage.contains("not active")) {
                return "deposits.error.account_inactive";
            }

            if (aMessage.contains("insufficient")) {
                return "deposits.error.insufficient_balance";
            }

            return "deposits.error.business_rule";
        }

        return "deposits.error.unexpected";
    }
}
