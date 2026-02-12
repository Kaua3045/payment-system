package com.payment.system.application.repositories;

import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.transactions.Transaction;

import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> transactionOfIdempotencyKey(String idempotencyKey);

    Optional<Transaction> transactionOfIdAndAccountId(String transactionId, String accountId);

    Pagination<Transaction> listAll(SearchQuery query);
}
