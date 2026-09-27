package com.payment.system.application.usecases.accounts.close;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.NotFoundException;

import java.util.Map;
import java.util.Objects;

public class DefaultCloseAccountUseCase extends CloseAccountUseCase {

    private final AccountRepository accountRepository;
    private final Metrics metrics;

    public DefaultCloseAccountUseCase(
            final AccountRepository accountRepository,
            final Metrics metrics,
            final ApplicationLogger aLogger
    ) {
        super(aLogger);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public void execute(final CloseAccountCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CloseAccountUseCase.class);
        }

        final var aAccount = this.accountRepository.accountOfId(input.accountId())
                .orElseThrow(NotFoundException.with(Account.class, input.accountId()));

        if (aAccount.getStatus().equals(AccountStatus.CLOSED)) {
            return;
        }

        aAccount.close();

        this.accountRepository.save(aAccount);
        this.metrics.incrementCounter("accounts_closed", 1, Map.of());
    }
}
