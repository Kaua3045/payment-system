package com.payment.system.domain.transactions;

import com.payment.system.domain.AggregateRoot;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.ValidationHandler;
import com.payment.system.domain.valueobjects.Money;

import java.time.Instant;
import java.util.Optional;

public class Transaction extends AggregateRoot<TransactionId> {

    private AccountId fromAccountId;
    private AccountId toAccountId;
    private PixKeyId pixKeyId;
    private Money amount;
    private TransactionStatus status;
    private String idempotencyKey;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    private Transaction(
            final TransactionId aTransactionId,
            final long aVersion,
            final AccountId aFromAccountId,
            final AccountId aToAccountId,
            final PixKeyId aPixKeyId,
            final Money aAmount,
            final TransactionStatus aStatus,
            final String aIdempotencyKey,
            final String aFailureReason,
            final Instant aCreatedAt,
            final Instant aUpdatedAt
    ) {
        super(aTransactionId, aVersion);
        this.setFromAccountId(aFromAccountId);
        this.setToAccountId(aToAccountId);
        this.setPixKeyId(aPixKeyId);
        this.setAmount(aAmount);
        this.setStatus(aStatus);
        this.setIdempotencyKey(aIdempotencyKey);
        this.setFailureReason(aFailureReason);
        this.setCreatedAt(aCreatedAt);
        this.setUpdatedAt(aUpdatedAt);
    }

    public static Transaction newTransaction(
            final AccountId fromAccountId,
            final AccountId toAccountId,
            final PixKeyId pixKeyId,
            final Money amount,
            final String idempotencyKey
    ) {
        final var aId = new TransactionId(IdentifierUtils.generateNewMonotonicULID());
        final var aNow = InstantUtils.now();
        return new Transaction(
                aId,
                0L,
                fromAccountId,
                toAccountId,
                pixKeyId,
                amount,
                TransactionStatus.PENDING,
                idempotencyKey,
                null,
                aNow,
                aNow
        );
    }

    public static Transaction with(
            final TransactionId aTransactionId,
            final long aVersion,
            final AccountId aFromAccountId,
            final AccountId aToAccountId,
            final PixKeyId aPixKeyId,
            final Money aAmount,
            final TransactionStatus aStatus,
            final String aIdempotencyKey,
            final String aFailureReason,
            final Instant aCreatedAt,
            final Instant aUpdatedAt
    ) {
        return new Transaction(
                aTransactionId,
                aVersion,
                aFromAccountId,
                aToAccountId,
                aPixKeyId,
                aAmount,
                aStatus,
                aIdempotencyKey,
                aFailureReason,
                aCreatedAt,
                aUpdatedAt
        );
    }

    public AccountId getFromAccountId() {
        return fromAccountId;
    }

    public AccountId getToAccountId() {
        return toAccountId;
    }

    public PixKeyId getPixKeyId() {
        return pixKeyId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void setFromAccountId(final AccountId fromAccountId) {
        this.assertArgumentNotNull(fromAccountId, "fromAccountId", "should not be null");
        this.fromAccountId = fromAccountId;
    }

    private void setToAccountId(final AccountId toAccountId) {
        this.assertArgumentNotNull(toAccountId, "toAccountId", "should not be null");
        this.toAccountId = toAccountId;
    }

    private void setPixKeyId(final PixKeyId pixKeyId) {
        this.assertArgumentNotNull(pixKeyId, "pixKeyId", "should not be null");
        this.pixKeyId = pixKeyId;
    }

    private void setAmount(final Money amount) {
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.amount = amount;
    }

    private void setStatus(final TransactionStatus status) {
        this.assertArgumentNotNull(status, "status", "should not be null");
        this.status = status;
    }

    private void setIdempotencyKey(final String idempotencyKey) {
        this.assertArgumentNotEmpty(idempotencyKey, "idempotencyKey", "should not be empty");
        this.idempotencyKey = idempotencyKey;
    }

    private void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    private void setCreatedAt(final Instant createdAt) {
        this.assertArgumentNotNull(createdAt, "createdAt", "should not be null");
        this.createdAt = createdAt;
    }

    private void setUpdatedAt(final Instant updatedAt) {
        this.assertArgumentNotNull(updatedAt, "updatedAt", "should not be null");
        this.updatedAt = updatedAt;
    }

    @Override
    public void validate(ValidationHandler aHandler) {

    }

    @Override
    public String toString() {
        return "Transaction(" +
                "id=" + getId().value().toString() +
                ", version=" + getVersion() +
                ", fromAccountId=" + fromAccountId.value().toString() +
                ", toAccountId=" + toAccountId.value().toString() +
                ", pixKeyId=" + pixKeyId.value().toString() +
                ", amount=" + amount.amount() +
                ", status=" + status.name() +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ')';
    }
}
