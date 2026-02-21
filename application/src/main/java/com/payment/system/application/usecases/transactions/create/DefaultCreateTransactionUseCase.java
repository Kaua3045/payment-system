package com.payment.system.application.usecases.transactions.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
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

public class DefaultCreateTransactionUseCase extends CreateTransactionUseCase {

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionManager transactionManager;
    private final Metrics metrics;

    public DefaultCreateTransactionUseCase(
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
    public CreateTransactionOutput execute(final CreateTransactionCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateTransactionUseCase.class);
        }

        final var aStartTime = System.currentTimeMillis();

        try {
            this.metrics.incrementCounter("pix_transfers_requested", 1);
            return this.transactionManager.execute(() -> {
                if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw DomainException.with("Amount must be greater than zero");
                }

                final var aFromAccount = this.accountRepository.accountOfId(input.fromAccountId())
                        .orElseThrow(NotFoundException.with(Account.class, input.fromAccountId()));

                if (!aFromAccount.getStatus().equals(AccountStatus.ACTIVE)) {
                    throw DomainException.with("From account is not active");
                }

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
                        aFromAccount.getId(),
                        aToAccount.getId(),
                        aPixKey.getId(),
                        new Money(input.amount()),
                        TransactionType.TRANSFER,
                        DepositSource.EXTERNAL,
                        input.idempotencyKey()
                );

                this.transactionRepository.save(aTransaction);

                aFromAccount.debit(input.amount());
                aToAccount.credit(input.amount());

                this.accountRepository.save(aFromAccount);
                this.accountRepository.save(aToAccount);

                aTransaction.complete();
                this.transactionRepository.save(aTransaction);

                this.metrics.incrementCounter("pix_transfers_processed", 1);
                this.metrics.incrementCounter("pix_transfers_amount_total", aTransaction.getAmount().amount().longValue());
                return CreateTransactionOutput.from(aTransaction);
            });
        } catch (final Exception ex) {
            this.transactionManager.execute(() -> {
                this.transactionRepository.transactionOfIdempotencyKey(input.idempotencyKey())
                        .ifPresent(tx -> {
                            tx.fail(ex.getMessage());
                            this.transactionRepository.save(tx);
                        });
                return null;
            });

            this.metrics.incrementCounter("pix_transfers_failed", 1);
            this.metrics.incrementCounter(resolveErrorMetric(ex), 1);
            throw ex;
        } finally {
            final var aDuration = System.currentTimeMillis() - aStartTime;
            this.metrics.recordTime("pix_transfers_latency", aDuration);
        }
    }

    @Generated
    private String resolveErrorMetric(final Exception ex) {
        if (ex instanceof NotFoundException notFound) {
            final var aMessage = notFound.getMessage().toLowerCase();

            if (aMessage.contains("account")) {
                return "pix_transfers_error_account_not_found";
            }

            if (aMessage.contains("pixkey")) {
                return "pix_transfers_error_pixkey_not_found";
            }

            return "pix_transfers_error_not_found";
        }

        if (ex instanceof DomainException domain) {
            final var aMessage = domain.getMessage().toLowerCase();

            if (aMessage.contains("not active")) {
                return "pix_transfers_error_account_inactive";
            }

            if (aMessage.contains("insufficient")) {
                return "pix_transfers_error_insufficient_balance";
            }

            return "pix_transfers_error_business_rule";
        }

        return "pix_transfers_error_unexpected";
    }
}
