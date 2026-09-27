package com.payment.system.application.usecases.transactions.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.helpers.ErrorClassifier;
import com.payment.system.application.helpers.ErrorType;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.ConflictException;
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
import java.util.Map;
import java.util.Objects;

public class DefaultCreateTransactionUseCase extends CreateTransactionUseCase {

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionManager transactionManager;
    private final Metrics metrics;
    private final TracerWrapper trace;

    public DefaultCreateTransactionUseCase(
            final AccountRepository accountRepository,
            final PixKeyRepository pixKeyRepository,
            final TransactionRepository transactionRepository,
            final TransactionManager transactionManager,
            final Metrics metrics,
            final ApplicationLogger logger, TracerWrapper trace
    ) {
        super(logger);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
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

                final var aPixKeyType = PixKeyType.from(input.pixKeyType())
                        .orElseThrow(() -> NotFoundException.with("PixKeyType %s not found".formatted(input.pixKeyType())));

                final var aKey = new PixKeyValueFactory().create(aPixKeyType, input.pixKey());

                return this.transactionManager.execute(() -> {
                    final var aFromAccount = span.runInSpan("db.account.find-source", () -> this.accountRepository.accountOfId(input.fromAccountId())
                            .orElseThrow(NotFoundException.with(Account.class, input.fromAccountId())));

                    if (!aFromAccount.getStatus().equals(AccountStatus.ACTIVE)) {
                        throw DomainException.with("From account is not active");
                    }

                    final var aPixKey = span.runInSpan("db.pixkey.find", () -> this.pixKeyRepository.pixKeyOfActiveByValue(aKey.value())
                            .orElseThrow(NotFoundException.with(PixKey.class, "value", input.pixKey())));

                    final var aToAccount = span.runInSpan("db.account.find-destination", () -> this.accountRepository.accountOfId(aPixKey.getAccountId().value().toString())
                            .orElseThrow(NotFoundException.with(Account.class, aPixKey.getAccountId().value().toString())));

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

                    aFromAccount.debit(input.amount());
                    aToAccount.credit(input.amount());

                    span.runInSpan("db.accounts.apply-transfer", () -> this.accountRepository.applyTransfer(aFromAccount, aToAccount));

                    aTransaction.complete();
                    span.runInSpan("db.transaction.save", () -> this.transactionRepository.save(aTransaction));

                    this.metrics.incrementCounter("application_usecase_invocations_total_success", 1, Map.of(
                            "usecase", "pix_transfer"
                    ));
                    this.metrics.incrementCounter("transaction_amount_total", aTransaction.getAmount().amount().longValue(),
                            Map.of("usecase", "pix_transfer"));

                    logger.info("event=pix_transfer_completed transactionId={} fromAccountId={} toAccountId={} amount={} idempotencyKey={}",
                            aTransaction.getId().value().toString(),
                            aFromAccount.getId().value().toString(),
                            aToAccount.getId().value().toString(),
                            aTransaction.getAmount().amount(),
                            aTransaction.getIdempotencyKey()
                    );
                    return CreateTransactionOutput.from(aTransaction);
                });
            } catch (final Exception ex) {
                if (ex instanceof ConflictException conflictException) {
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

        if (ex instanceof DomainException domain) {
            final var aMessage = domain.getMessage().toLowerCase();

            if (aMessage.contains("not active")) {
                return "account_inactive";
            }

            if (aMessage.contains("insufficient")) {
                return "insufficient_balance";
            }

            return "business_rule";
        }

        return "unexpected";
    }
}
