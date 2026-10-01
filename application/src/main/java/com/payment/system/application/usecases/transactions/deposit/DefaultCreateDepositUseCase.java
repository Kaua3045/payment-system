package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.exceptions.AccountIsNotActiveException;
import com.payment.system.application.exceptions.PixKeyIsNotActiveException;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.helpers.ErrorClassifier;
import com.payment.system.application.helpers.ErrorType;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.ConflictException;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.Generated;
import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public class DefaultCreateDepositUseCase extends CreateDepositUseCase {

    private final AccountGateway accountGateway;
    private final PixKeyGateway pixKeyGateway;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionManager transactionManager;
    private final Metrics metrics;

    public DefaultCreateDepositUseCase(
            final AccountGateway accountGateway,
            final PixKeyGateway pixKeyGateway,
            final TransactionRepository transactionRepository,
            final LedgerRepository ledgerRepository,
            final TransactionManager transactionManager,
            final Metrics metrics,
            final ApplicationLogger logger
    ) {
        super(logger);
        this.accountGateway = Objects.requireNonNull(accountGateway);
        this.pixKeyGateway = Objects.requireNonNull(pixKeyGateway);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.ledgerRepository = Objects.requireNonNull(ledgerRepository);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public CreateDepositOutput execute(final CreateDepositCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateDepositUseCase.class);
        }

        final var aStartTime = System.currentTimeMillis();

        logger.info("event=deposit_requested pixKeyType={} source={} amount={} idempotencyKey={}",
                input.pixKeyType(), input.source(), input.amount(), input.idempotencyKey());

        try {
            this.metrics.incrementCounter("application_usecase_invocations_total", 1, Map.of("usecase", "deposit_create"));
            return this.transactionManager.execute(() -> {
                if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw DomainException.with("Amount must be greater than zero");
                }

                final var aSource = DepositSource.from(input.source())
                        .orElseThrow(() -> NotFoundException.with("DepositSource %s not found".formatted(input.source())));

                final var aPixKey = this.pixKeyGateway.pixKeyOfActiveByValue(input.pixKeyType(), input.pixKey());

                if (!aPixKey.active()) {
                    throw new PixKeyIsNotActiveException(input.pixKey());
                }

                final var aToAccount = this.accountGateway.accountOfId(aPixKey.accountId());

                if (!aToAccount.status().equals(AccountStatus.ACTIVE)) {
                    throw new AccountIsNotActiveException("To");
                }

                final var aTransaction = Transaction.newTransaction(
                        AccountId.system(),
                        aToAccount.accountId(),
                        aPixKey.pixKeyId(),
                        new Money(input.amount()),
                        TransactionType.TRANSFER,
                        aSource,
                        input.idempotencyKey()
                );

                aTransaction.complete();
                this.transactionRepository.save(aTransaction);

                final var aCreditEntry = LedgerEntry.newCredit(aToAccount.accountId(), aTransaction.getId(), input.amount());
                this.ledgerRepository.save(aCreditEntry);

                this.metrics.incrementCounter("application_usecase_invocations_total_success", 1, Map.of("usecase", "deposit_create"));
                this.metrics.incrementCounter("transaction_amount_total", aTransaction.getAmount().amount().longValue(),
                        Map.of("usecase", "deposit_create"));

                logger.info("event=deposit_completed transactionId={} toAccountId={} amount={} idempotencyKey={}",
                        aTransaction.getId().value().toString(),
                        aToAccount.accountId().value().toString(),
                        aTransaction.getAmount().amount(),
                        aTransaction.getIdempotencyKey()
                );
                return CreateDepositOutput.from(aTransaction);
            });
        } catch (final Exception ex) {
            if (ex instanceof ConflictException conflictException) {
                logger.warn("event=deposit_conflict reason={} idempotencyKey={}",
                        conflictException.getMessage(),
                        input.idempotencyKey()
                );
                this.metrics.incrementCounter("application_usecase_errors_total", 1, Map.of(
                        "usecase", "deposit_create",
                        "error_code", "conflict_version_or_idempotency_key"
                ));
                throw conflictException;
            }

            this.transactionManager.execute(() -> {
                this.transactionRepository.transactionOfIdempotencyKey(input.idempotencyKey())
                        .ifPresent(tx -> {
                            tx.fail(ex.getMessage());
                            this.transactionRepository.save(tx);
                        });
                return null;
            });

            final var aErrorType = ErrorClassifier.classify(ex);

            if (ErrorType.IsBusiness(aErrorType)) {
                logger.warn("event=deposit_failed reason={} idempotencyKey={}",
                        ex.getMessage(),
                        input.idempotencyKey()
                );
            } else {
                logger.error("event=deposit_error idempotencyKey={}", input.idempotencyKey(), ex);
            }

            this.metrics.incrementCounter("application_usecase_errors_total", 1, Map.of(
                    "usecase", "deposit_create",
                    "error_code", resolveErrorMetric(ex)
            ));

            throw ex;
        } finally {
            final var aDuration = System.currentTimeMillis() - aStartTime;
            this.metrics.incrementCounter("application_usecase_duration", aDuration, Map.of("usecase", "deposit_create"));
        }
    }

    @Generated
    private String resolveErrorMetric(final Exception ex) {
        if (ex instanceof NotFoundException notFound) {
            final var aMessage = notFound.getMessage().toLowerCase();

            if (aMessage.contains("account")) {
                return "account_not_found";
            }

            if (aMessage.contains("pixkey")) {
                return "pixkey_not_found";
            }

            return "not_found";
        }

        if (ex instanceof AccountIsNotActiveException) {
            return "account_inactive";
        }

        if (ex instanceof InsufficientFundsException) {
            return "insufficient_balance";
        }

        if (ex instanceof DomainException) {
            return "business_rule";
        }

        return "unexpected";
    }
}
