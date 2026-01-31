package com.payment.system.application.usecases.accounts.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;

import java.util.Objects;

public class DefaultCreateAccountUseCase extends CreateAccountUseCase {

    private final AccountRepository accountRepository;

    public DefaultCreateAccountUseCase(final AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
    }

    @Override
    public CreateAccountOutput execute(final CreateAccountCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateAccountUseCase.class);
        }

        final var aAccount = Account.newAccount(input.userId());

        this.accountRepository.save(aAccount);

        return CreateAccountOutput.from(aAccount);
    }
}
