package com.payment.system.application.repositories;

import com.payment.system.domain.transactions.Transaction;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
