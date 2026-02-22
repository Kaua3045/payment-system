package com.payment.system.application.usecases.accounts.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.accounts.Account;

import java.util.Objects;

public class DefaultCreateAccountUseCase extends CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final Metrics metrics;

    public DefaultCreateAccountUseCase(
            final AccountRepository accountRepository,
            final Metrics metrics,
            final ApplicationLogger logger
    ) {
        super(logger);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public CreateAccountOutput execute(final CreateAccountCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreateAccountUseCase.class);
        }

        final var aStartTime = System.currentTimeMillis();

        logger.info("event=account_create_requested userId={}", input.userId());

        final var aAccount = Account.newAccount(input.userId());

        this.accountRepository.save(aAccount);

        final var aDuration = System.currentTimeMillis() - aStartTime;

        logger.info("event=account_create_completed accountId={} userId={}",
                aAccount.getId().value().toString(),
                input.userId()
        );

        this.metrics.incrementCounter("accounts_created_total", 1);
        this.metrics.recordTime("accounts_created_latency", aDuration);

        return CreateAccountOutput.from(aAccount);
    }
}
