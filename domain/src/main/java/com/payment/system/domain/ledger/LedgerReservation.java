package com.payment.system.domain.ledger;

import com.payment.system.domain.AggregateRoot;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.transactions.TransactionId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.ValidationHandler;

import java.math.BigDecimal;
import java.time.Instant;

public class LedgerReservation extends AggregateRoot<LedgerReservationId> {

    private TransactionId transactionId;
    private AccountId accountId;
    private BigDecimal amount;
    private Instant createdAt;
    private Instant expiresAt;
    private ReservationStatus status;

    private LedgerReservation(
            final LedgerReservationId ledgerReservationId,
            final long version,
            final TransactionId transactionId,
            final AccountId accountId,
            final BigDecimal amount,
            final Instant createdAt,
            final Instant expiresAt,
            final ReservationStatus status
    ) {
        super(ledgerReservationId, version);
        this.setTransactionId(transactionId);
        this.setAccountId(accountId);
        this.setAmount(amount);
        this.setCreatedAt(createdAt);
        this.setExpiresAt(expiresAt);
        this.setStatus(status);
    }

    @Override
    public void validate(ValidationHandler aHandler) {
    }

    public static LedgerReservation with(
            final LedgerReservationId ledgerReservationId,
            final long version,
            final TransactionId transactionId,
            final AccountId accountId,
            final BigDecimal amount,
            final Instant createdAt,
            final Instant expiresAt,
            final ReservationStatus status
    ) {
        return new LedgerReservation(
                ledgerReservationId,
                version,
                transactionId,
                accountId,
                amount,
                createdAt,
                expiresAt,
                status
        );
    }

    public static LedgerReservation newReservation(
            final TransactionId transactionId,
            final AccountId accountId,
            final BigDecimal amount,
            final Instant expiresAt
    ) {
        return new LedgerReservation(
                new LedgerReservationId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                transactionId,
                accountId,
                amount,
                InstantUtils.now(),
                expiresAt,
                ReservationStatus.PENDING
        );
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    private void setTransactionId(final TransactionId transactionId) {
        this.assertArgumentNotNull(transactionId, "transactionId", "should not be null");
        this.transactionId = transactionId;
    }

    private void setAccountId(final AccountId accountId) {
        this.assertArgumentNotNull(accountId, "accountId", "should not be null");
        this.accountId = accountId;
    }

    private void setAmount(final BigDecimal amount) {
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.amount = amount;
    }

    private void setCreatedAt(final Instant createdAt) {
        this.assertArgumentNotNull(createdAt, "createdAt", "should not be null");
        this.createdAt = createdAt;
    }

    private void setExpiresAt(final Instant expiresAt) {
        this.assertArgumentNotNull(expiresAt, "expiresAt", "should not be null");
        this.expiresAt = expiresAt;
    }

    private void setStatus(final ReservationStatus status) {
        this.assertArgumentNotNull(status, "status", "should not be null");
        this.status = status;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
