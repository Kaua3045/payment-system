package com.payment.system.application.usecases.transactions.retrieve.list;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;

public abstract class ListTransactionsUseCase extends UseCase<SearchQuery, Pagination<ListTransactionsOutput>> {

    protected ListTransactionsUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
