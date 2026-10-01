package com.payment.system.domain.ledger;

import com.payment.system.domain.AggregateRoot;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.transactions.TransactionId;

import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.ValidationHandler;

import java.math.BigDecimal;
import java.time.Instant;

// This aggregate contains the ledger entries for an account.
// It is used to calculate the balance of an account. The balance is calculated by summing the amounts of all ledger entries for an account.
// The ledger entries are stored in the database and are immutable.
public class LedgerEntry extends AggregateRoot<LedgerEntryId> {

    private AccountId accountId;
    private TransactionId transactionId;
    private BigDecimal amount;
    private LedgerType type;
    private Instant createdAt;

    private LedgerEntry(
            final LedgerEntryId id,
            final long version,
            final AccountId accountId,
            final TransactionId transactionId,
            final BigDecimal amount,
            final LedgerType type,
            final Instant createdAt
    ) {
        super(id, version);
        this.setAccountId(accountId);
        this.setTransactionId(transactionId);
        this.setAmount(amount);
        this.setType(type);
        this.setCreatedAt(createdAt);
    }

    @Override
    public void validate(ValidationHandler aHandler) {
    }

    public static LedgerEntry newDebit(
            final AccountId accountId,
            final TransactionId transactionId,
            final BigDecimal amount
    ) {
        return new LedgerEntry(
                new LedgerEntryId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                accountId,
                transactionId,
                amount.negate(),
                LedgerType.DEBIT,
                InstantUtils.now()
        );
    }

    public static LedgerEntry newCredit(
            final AccountId accountId,
            final TransactionId transactionId,
            final BigDecimal amount
    ) {
        return new LedgerEntry(
                new LedgerEntryId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                accountId,
                transactionId,
                amount,
                LedgerType.CREDIT,
                InstantUtils.now()
        );
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LedgerType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private void setAccountId(final AccountId accountId) {
        this.assertArgumentNotNull(accountId, "accountId", "should not be null");
        this.accountId = accountId;
    }

    private void setTransactionId(final TransactionId transactionId) {
        this.assertArgumentNotNull(transactionId, "transactionId", "should not be null");
        this.transactionId = transactionId;
    }

    private void setAmount(final BigDecimal amount) {
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.amount = amount;
    }

    private void setType(final LedgerType type) {
        this.assertArgumentNotNull(type, "type", "should not be null");
        this.type = type;
    }

    private void setCreatedAt(final Instant createdAt) {
        this.assertArgumentNotNull(createdAt, "createdAt", "should not be null");
        this.createdAt = createdAt;
    }
}
