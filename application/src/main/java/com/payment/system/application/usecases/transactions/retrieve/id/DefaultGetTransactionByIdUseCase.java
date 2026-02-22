package com.payment.system.application.usecases.transactions.retrieve.id;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.transactions.Transaction;

import java.util.Objects;

public class DefaultGetTransactionByIdUseCase extends GetTransactionByIdUseCase {

    private final TransactionRepository transactionRepository;

    public DefaultGetTransactionByIdUseCase(final TransactionRepository transactionRepository, final ApplicationLogger logger) {
        super(logger);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    @Override
    public GetTransactionByIdOutput execute(GetTransactionByIdCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(GetTransactionByIdUseCase.class);
        }

        return this.transactionRepository.transactionOfIdAndAccountId(input.transactionId(), input.accountId())
                .map(GetTransactionByIdOutput::from)
                .orElseThrow(NotFoundException.with(Transaction.class, input.transactionId()));
    }
}
