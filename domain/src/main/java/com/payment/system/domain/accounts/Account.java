package com.payment.system.domain.accounts;

import com.payment.system.domain.AggregateRoot;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.ValidationHandler;
import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public class Account extends AggregateRoot<AccountId> {

    private String userId;
    private Money balance;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    private Account(
            final AccountId aAccountId,
            final long aVersion,
            final String aUserId,
            final Money aBalance,
            final AccountStatus aStatus,
            final Instant aCreatedAt,
            final Instant aUpdatedAt,
            final Instant aClosedAt
    ) {
        super(aAccountId, aVersion);
        this.setUserId(aUserId);
        this.setBalance(aBalance);
        this.setStatus(aStatus);
        this.setCreatedAt(aCreatedAt);
        this.setUpdatedAt(aUpdatedAt);
        this.setClosedAt(aClosedAt);
    }

    public static Account newAccount(
            final String aUserId
    ) {
        final var aId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aNow = InstantUtils.now();
        return new Account(
                aId,
                0L,
                aUserId,
                Money.zero(),
                AccountStatus.ACTIVE,
                aNow,
                aNow,
                null
        );
    }

    public static Account with(
            final AccountId aAccountId,
            final long aVersion,
            final String aUserId,
            final Money aBalance,
            final AccountStatus aStatus,
            final Instant aCreatedAt,
            final Instant aUpdatedAt,
            final Instant aClosedAt
    ) {
        return new Account(
                aAccountId,
                aVersion,
                aUserId,
                aBalance,
                aStatus,
                aCreatedAt,
                aUpdatedAt,
                aClosedAt
        );
    }

    public void debit(final BigDecimal aAmount) {
        if (balance.amount().compareTo(aAmount) < 0) {
            throw ValidationException.with("Insufficient funds");
        }
        this.setBalance(balance.subtract(new Money(aAmount)));
        this.setUpdatedAt(InstantUtils.now());
    }

    public void credit(final BigDecimal aAmount) {
        this.setBalance(balance.add(new Money(aAmount)));
        this.setUpdatedAt(InstantUtils.now());
    }

    public String getUserId() {
        return userId;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    private void setUserId(final String userId) {
        this.assertArgumentNotEmpty(userId, "userId", "should not be empty");
        this.userId = userId;
    }

    private void setBalance(final Money balance) {
        this.assertArgumentNotNull(balance, "balance", "should not be null");
        this.balance = balance;
    }

    private void setStatus(final AccountStatus status) {
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

    private void setClosedAt(final Instant closedAt) {
        this.closedAt = closedAt;
    }

    @Override
    public String toString() {
        return "Account(" +
                "id=" + getId() +
                ", version=" + getVersion() +
                ", userId='" + userId + '\'' +
                ", balance=" + balance.amount() +
                ", status=" + status.name() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", closedAt=" + closedAt +
                ')';
    }

    @Override
    public void validate(ValidationHandler aHandler) {
    }
}
