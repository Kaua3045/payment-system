package com.payment.system.application.usecases.transactions.retrieve.list;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;

import java.util.Objects;

public class DefaultListTransactionsUseCase extends ListTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public DefaultListTransactionsUseCase(final TransactionRepository transactionRepository, final ApplicationLogger logger) {
        super(logger);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    @Override
    public Pagination<ListTransactionsOutput> execute(final SearchQuery input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(ListTransactionsUseCase.class);
        }

        return this.transactionRepository.listAll(input)
                .map(ListTransactionsOutput::from);
    }
}
