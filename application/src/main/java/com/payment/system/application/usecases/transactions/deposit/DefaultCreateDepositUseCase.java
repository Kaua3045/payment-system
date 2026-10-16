package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.exceptions.AccountIsNotActiveException;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.exceptions.NoStackTraceException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.valueobjects.Money;

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

        logger.info("event=deposit_requested pixKeyType={} pixKey={} source={} amount={} idempotencyKey={}",
                input.pixKeyType(), input.pixKey(), input.source(), input.amount(), input.idempotencyKey());

        try {
            if (input.amount().isLessThanOrEqual(Money.zero())) {
                throw DomainException.with("Amount must be greater than zero");
            }

            final var aSource = DepositSource.from(input.source())
                    .orElseThrow(() -> NotFoundException.with("DepositSource %s not found".formatted(input.source())));

            final var aPixKey = this.pixKeyGateway.pixKeyOfActiveByValue(input.pixKeyType(), input.pixKey());

            final var aToAccount = this.accountGateway.accountOfId(aPixKey.accountId());

            if (!aToAccount.status().equals(AccountStatus.ACTIVE)) {
                throw new AccountIsNotActiveException("To");
            }

            final var aTransaction = Transaction.newTransaction(
                    AccountId.system(),
                    aToAccount.accountId(),
                    aPixKey.pixKeyId(),
                    input.amount(),
                    TransactionType.TRANSFER,
                    aSource,
                    input.idempotencyKey()
            );

            final var aOutput = this.transactionManager.execute(() -> {
                aTransaction.complete();
                this.transactionRepository.save(aTransaction);

                final var aCreditEntry = LedgerEntry.newCredit(aToAccount.accountId(), aTransaction.getId(), input.amount());
                this.ledgerRepository.save(aCreditEntry);

                return CreateDepositOutput.from(aTransaction);
            });

            this.metrics.incrementCounter("application_usecase_invocations_total", 1, Map.of(
                    "usecase", "deposit_create",
                    "outcome", "success",
                    "error_code", "none"
            ));

            logger.info("event=deposit_completed transactionId={} toAccountId={} amount={} idempotencyKey={}",
                    aTransaction.getId().value().toString(),
                    aToAccount.accountId().value().toString(),
                    aTransaction.getAmount().amount(),
                    aTransaction.getIdempotencyKey()
            );

            return aOutput;
        } catch (final Exception ex) {
            this.metrics.incrementCounter("application_usecase_invocations_total", 1, Map.of(
                    "usecase", "deposit_create",
                    "outcome", "error",
                    "error_code", ex instanceof NoStackTraceException noStackTrace ? noStackTrace.getErrorCode() : "unexpected"
            ));

            if (ex instanceof InternalErrorException internalError) {
                logger.error("event=deposit_error errorCode={} reason={} idempotencyKey={}",
                        internalError.getErrorCode(),
                        internalError.getMessage(),
                        input.idempotencyKey(),
                        ex
                );
            } else if (ex instanceof NoStackTraceException noStackTrace) {
                logger.warn("event=deposit_rejected errorCode={} reason={} idempotencyKey={}",
                        noStackTrace.getErrorCode(),
                        noStackTrace.getMessage(),
                        input.idempotencyKey()
                );
            } else {
                logger.error("event=deposit_error_unexpected reason={} idempotencyKey={}",
                        ex.getMessage(),
                        input.idempotencyKey(),
                        ex
                );
            }

            throw ex;
        } finally {
            final var aDuration = System.currentTimeMillis() - aStartTime;
            this.metrics.incrementCounter("application_usecase_duration", aDuration, Map.of("usecase", "deposit_create"));
        }
    }
}
