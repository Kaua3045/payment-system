package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.application.wrapper.Metrics;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;

import java.util.Map;
import java.util.Objects;

public class DefaultCreatePixKeyUseCase extends CreatePixKeyUseCase {

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;
    private final Metrics metrics;

    public DefaultCreatePixKeyUseCase(
            final PixKeyRepository pixKeyRepository,
            final AccountRepository accountRepository,
            final Metrics metrics,
            final ApplicationLogger logger
    ) {
        super(logger);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public CreatePixKeyOutput execute(final CreatePixKeyCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreatePixKeyUseCase.class);
        }

        final var aStartTime = System.currentTimeMillis();

        logger.info("event=pix_key_create_requested pixKeyType={} accountId={}", input.type(), input.accountId());

        this.metrics.incrementCounter("application_usecase_invocations_total", 1, Map.of("usecase", "pixkey_create"));

        final var aExistsKey = this.pixKeyRepository.existsByValue(input.value());

        if (aExistsKey) {
            throw DomainException.with("Pix key with value %s already exists".formatted(input.value()));
        }

        final var aType = PixKeyType.from(input.type())
                .orElseThrow(() -> NotFoundException.with("Pix key type %s not found".formatted(input.type())));

        final var aAccount = this.accountRepository.accountOfId(input.accountId())
                .orElseThrow(NotFoundException.with(Account.class, input.accountId()));

        final var aPixKeyValueFactory = new PixKeyValueFactory();

        final var aPixKey = PixKey.newPixKey(
                aPixKeyValueFactory.create(aType, input.value()),
                aAccount.getId()
        );

        this.pixKeyRepository.save(aPixKey);

        final var aDuration = System.currentTimeMillis() - aStartTime;

        logger.info("event=pix_key_create_completed pixKeyId={} pixKeyType={} accountId={}",
                aPixKey.getId().value().toString(),
                aPixKey.getKey().type().name(),
                aPixKey.getAccountId().value().toString()
        );

        this.metrics.incrementCounter("application_usecase_invocations_total_success", 1, Map.of(
                "usecase", "pixkey_create",
                "pixkey_type", aType.name().toLowerCase()
        ));
        this.metrics.recordTime("application_usecase_duration", aDuration, Map.of("usecase", "pixkey_create"));

        return CreatePixKeyOutput.from(aPixKey);
    }
}
