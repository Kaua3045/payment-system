package com.payment.system.application.usecases.transactions.create;

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
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.ConflictException;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.ledger.LedgerReservation;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.Generated;
import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultCreateTransactionUseCase extends CreateTransactionUseCase {

    private final AccountGateway accountGateway;
    private final PixKeyGateway pixKeyGateway;

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    private final TransactionManager transactionManager;

    private final Metrics metrics;
    private final TracerWrapper trace;

    public DefaultCreateTransactionUseCase(
            final AccountGateway accountGateway,
            final PixKeyGateway pixKeyGateway,
            final TransactionRepository transactionRepository,
            final LedgerRepository ledgerRepository,
            final TransactionManager transactionManager,
            final Metrics metrics,
            final ApplicationLogger logger,
            final TracerWrapper trace
    ) {
        super(logger);
        this.accountGateway = Objects.requireNonNull(accountGateway);
        this.pixKeyGateway = Objects.requireNonNull(pixKeyGateway);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.ledgerRepository = Objects.requireNonNull(ledgerRepository);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.metrics = Objects.requireNonNull(metrics);
        this.trace = trace;
    }

    @Override
    public CreateTransactionOutput execute(final CreateTransactionCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateTransactionUseCase.class);
        }

        return this.trace.traceWithReturn("create-transaction-usecase", (span) -> {
            final var aStartTime = System.currentTimeMillis();

            logger.info("event=pix_transfer_requested fromAccountId={} pixKeyType={} amount={} idempotencyKey={}",
                    input.fromAccountId(), input.pixKeyType(), input.amount(), input.idempotencyKey());

            try {
                this.metrics.incrementCounter("application_usecase_invocations_total", 1, Map.of(
                        "usecase", "pix_transfer"
                ));

                if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw DomainException.with("Amount must be greater than zero");
                }

                final var aPixKeyResponse = span.runInSpan("gateway.pixkey.find", () -> this.pixKeyGateway.pixKeyOfActiveByValue(input.pixKeyType(), input.pixKey()));

                if (!aPixKeyResponse.active()) {
                    throw new PixKeyIsNotActiveException(input.pixKey());
                }

                final var aFromAccount = span.runInSpan("gateway.account.find-source", () -> this.accountGateway.accountOfId(input.fromAccountId()));

                if (!aFromAccount.status().equals(AccountStatus.ACTIVE)) {
                    throw new AccountIsNotActiveException("From");
                }

                final var aToAccount = span.runInSpan("gateway.account.find-destination", () -> this.accountGateway.accountOfId(aPixKeyResponse.accountId()));

                if (!aToAccount.status().equals(AccountStatus.ACTIVE)) {
                    throw new AccountIsNotActiveException("To");
                }

                // TODO: [FRAUDE] Verificação síncrona de risco antes da reserva

                final var aTransaction = Transaction.newTransaction(
                        aFromAccount.accountId(),
                        aToAccount.accountId(),
                        aPixKeyResponse.pixKeyId(),
                        new Money(input.amount()),
                        TransactionType.TRANSFER,
                        DepositSource.EXTERNAL,
                        input.idempotencyKey()
                );

                final var aReservation = LedgerReservation.newReservation(
                        aTransaction.getId(),
                        aFromAccount.accountId(),
                        input.amount(),
                        Instant.now().plus(10, ChronoUnit.MINUTES)
                );

                return this.transactionManager.execute(() -> {
                    this.ledgerRepository.createReservation(aReservation);

                    aTransaction.complete();
                    span.runInSpan("db.transaction.save", () -> this.transactionRepository.save(aTransaction));

                    final var debitEntry = LedgerEntry.newDebit(aFromAccount.accountId(), aTransaction.getId(), input.amount());
                    final var creditEntry = LedgerEntry.newCredit(aToAccount.accountId(), aTransaction.getId(), input.amount());

                    this.ledgerRepository.saveAll(List.of(debitEntry, creditEntry));

                    // 3. CONFIRMAÇÃO DA RESERVA
                    this.ledgerRepository.confirmReservation(aTransaction.getId());

                    // TODO: [FRAUDE] Disparar evento para análise assíncrona

                    this.metrics.incrementCounter("application_usecase_invocations_total_success", 1, Map.of(
                            "usecase", "pix_transfer"
                    ));
                    this.metrics.incrementCounter("transaction_amount_total", aTransaction.getAmount().amount().longValue(),
                            Map.of("usecase", "pix_transfer"));

                    logger.info("event=pix_transfer_completed transactionId={} fromAccountId={} toAccountId={} amount={} idempotencyKey={}",
                            aTransaction.getId().value().toString(),
                            aFromAccount.accountId().value().toString(),
                            aToAccount.accountId().value().toString(),
                            aTransaction.getAmount().amount(),
                            aTransaction.getIdempotencyKey()
                    );
                    return CreateTransactionOutput.from(aTransaction);
                });
            } catch (final Exception ex) {
                if (ex instanceof ConflictException conflictException) {
                    // TODO hoje apos as modificacoes, so se for idempotency key
                    logger.warn("event=pix_transfer_conflict reason={} idempotencyKey={}",
                            conflictException.getMessage(),
                            input.idempotencyKey()
                    );
                    this.metrics.incrementCounter("application_usecase_errors_total", 1, Map.of(
                            "usecase", "pix_transfer",
                            "error_code", "conflict_version_or_idempotency_key"
                    ));
                    throw conflictException;
                }

                final var aErrorType = ErrorClassifier.classify(ex);

                if (ErrorType.IsBusiness(aErrorType)) {
                    logger.warn("event=pix_transfer_failed reason={} idempotencyKey={}",
                            ex.getMessage(),
                            input.idempotencyKey()
                    );
                } else {
                    logger.error("event=pix_transfer_error idempotencyKey={}", input.idempotencyKey(), ex);
                }

                this.metrics.incrementCounter("application_usecase_errors_total", 1, Map.of(
                        "usecase", "pix_transfer",
                        "error_code", resolveErrorMetric(ex)
                ));
                throw ex;
            } finally {
                final var aDuration = System.currentTimeMillis() - aStartTime;
                this.metrics.recordTime("application_usecase_duration", aDuration, Map.of(
                        "usecase", "pix_transfer"
                ));
            }
        });
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

        if (ex instanceof InsufficientFundsException) {
            return "insufficient_balance";
        }

        if (ex instanceof AccountIsNotActiveException) {
            return "account_inactive";
        }

        if (ex instanceof DomainException) {
            return "business_rule";
        }

        return "unexpected";
    }
}
