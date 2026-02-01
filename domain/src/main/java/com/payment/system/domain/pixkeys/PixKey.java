package com.payment.system.domain.pixkeys;

import com.payment.system.domain.AggregateRoot;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.ValidationHandler;

import java.time.Instant;
import java.util.Optional;

public class PixKey extends AggregateRoot<PixKeyId> {

    private PixKeyType type;
    private String value;
    private AccountId accountId;
    private PixKeyStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private PixKey(
            final PixKeyId aPixKeyId,
            final long aVersion,
            final PixKeyType aType,
            final String aValue,
            final AccountId anAccountId,
            final PixKeyStatus aStatus,
            final Instant aCreatedAt,
            final Instant anUpdatedAt,
            final Instant aDeletedAt
    ) {
        super(aPixKeyId, aVersion);
        this.setType(aType);
        this.setValue(aValue);
        this.setAccountId(anAccountId);
        this.setStatus(aStatus);
        this.setCreatedAt(aCreatedAt);
        this.setUpdatedAt(anUpdatedAt);
        this.setDeletedAt(aDeletedAt);
    }

    public static PixKey newPixKey(
            final PixKeyType aType,
            final String aValue,
            final AccountId aAccountId
    ) {
        final var aId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var aNow = InstantUtils.now();
        return new PixKey(
                aId,
                0L,
                aType,
                aValue,
                aAccountId,
                PixKeyStatus.ACTIVE,
                aNow,
                aNow,
                null
        );
    }

    public static PixKey with(
            final PixKeyId aPixKeyId,
            final long aVersion,
            final PixKeyType aType,
            final String aValue,
            final AccountId anAccountId,
            final PixKeyStatus aStatus,
            final Instant aCreatedAt,
            final Instant anUpdatedAt,
            final Instant aDeletedAt
    ) {
        return new PixKey(
                aPixKeyId,
                aVersion,
                aType,
                aValue,
                anAccountId,
                aStatus,
                aCreatedAt,
                anUpdatedAt,
                aDeletedAt
        );
    }

    public PixKeyType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public PixKeyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    private void setType(final PixKeyType type) {
        this.assertArgumentNotNull(type, "type", "should not be null");
        this.type = type;
    }

    private void setValue(final String value) {
        this.assertArgumentNotEmpty(value, "value", "should not be empty");
        this.value = value;
    }

    private void setAccountId(final AccountId accountId) {
        this.assertArgumentNotNull(accountId, "accountId", "should not be null");
        this.accountId = accountId;
    }

    private void setStatus(final PixKeyStatus status) {
        this.assertArgumentNotNull(status, "status", "should not be null");
        this.status = status;
    }

    private void setCreatedAt(final Instant createdAt) {
        this.assertArgumentNotNull(createdAt, "createdAt", "should not be null");
        this.createdAt = createdAt;
    }

    private void setUpdatedAt(final Instant updatedAt) {
        this.assertArgumentNotNull(updatedAt, "updatedAt", "should not be null");
        this.updatedAt = updatedAt;
    }

    private void setDeletedAt(final Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public void validate(ValidationHandler aHandler) {}

    @Override
    public String toString() {
        return "PixKey(" +
                "id=" + getId().value().toString() +
                ", type='" + type.name() + '\'' +
                ", value='" + value + '\'' +
                ", accountId=" + accountId.value().toString() +
                ", status=" + status.name() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                ')';
    }
}
