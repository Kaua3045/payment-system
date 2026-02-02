package com.payment.system.application.usecases.transactions.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class DefaultCreateTransactionUseCase extends CreateTransactionUseCase {

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionManager transactionManager;

    public DefaultCreateTransactionUseCase(
            final AccountRepository accountRepository,
            final PixKeyRepository pixKeyRepository,
            final TransactionRepository transactionRepository,
            final TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }

    @Override
    public CreateTransactionOutput execute(final CreateTransactionCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateTransactionUseCase.class);
        }

        if (this.transactionRepository.existsByIdempotencyKey(input.idempotencyKey())) {
            throw DomainException.with("Transaction with idempotencyKey %s already exists".formatted(input.idempotencyKey()));
        }

        return this.transactionManager.execute(() -> {
            if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw DomainException.with("Amount must be greater than zero");
            }

            final var aFromAccount = this.accountRepository.accountOfId(input.fromAccountId())
                    .orElseThrow(NotFoundException.with(Account.class, input.fromAccountId()));

            if (!aFromAccount.getStatus().equals(AccountStatus.ACTIVE)) {
                throw DomainException.with("From account is not active");
            }

            final var aPixKey = this.pixKeyRepository.pixKeyOfActiveByValue(input.pixKey())
                    .orElseThrow(NotFoundException.with(PixKey.class, "value", input.pixKey()));

            final var aToAccount = this.accountRepository.accountOfId(aPixKey.getAccountId().value().toString())
                    .orElseThrow(NotFoundException.with(Account.class, aPixKey.getAccountId().value().toString()));

            if (!aToAccount.getStatus().equals(AccountStatus.ACTIVE)) {
                throw DomainException.with("To account is not active");
            }

            final var aTransaction = Transaction.newTransaction(
                    aFromAccount.getId(),
                    aToAccount.getId(),
                    aPixKey.getId(),
                    new Money(input.amount()),
                    input.idempotencyKey()
            );

            this.transactionRepository.save(aTransaction);

            aFromAccount.debit(input.amount());
            aToAccount.credit(input.amount());

            this.accountRepository.save(aFromAccount);
            this.accountRepository.save(aToAccount);

            aTransaction.complete();
            this.transactionRepository.save(aTransaction);

            return CreateTransactionOutput.from(aTransaction);
        });
    }
}
