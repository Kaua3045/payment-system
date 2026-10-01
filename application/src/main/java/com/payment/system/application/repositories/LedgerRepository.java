package com.payment.system.application.repositories;

import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.ledger.LedgerReservation;
import com.payment.system.domain.transactions.TransactionId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository {
    void save(LedgerEntry entry);
    void saveAll(List<LedgerEntry> entries);

    BigDecimal calculateBalance(AccountId accountId);

    void createReservation(LedgerReservation reservation);
    void confirmReservation(TransactionId transactionId);
    void cancelReservation(TransactionId transactionId);

    Optional<LedgerReservation> findReservationByTransactionId(TransactionId transactionId);
}
