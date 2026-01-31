package com.payment.system.application.usecases.accounts.retrieve.id;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.NotFoundException;

import java.util.Objects;

public class DefaultGetAccountByIdUseCase extends GetAccountByIdUseCase {

    private final AccountRepository accountRepository;

    public DefaultGetAccountByIdUseCase(final AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
    }

    @Override
    public GetAccountByIdOutput execute(final GetAccountByIdCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(GetAccountByIdUseCase.class);
        }

        return this.accountRepository.accountOfId(input.id())
                .map(GetAccountByIdOutput::from)
                .orElseThrow(NotFoundException.with(Account.class, input.id()));
    }
}
