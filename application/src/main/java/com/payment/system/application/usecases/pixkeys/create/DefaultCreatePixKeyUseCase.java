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
import com.payment.system.domain.utils.Generated;

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

        this.metrics.incrementCounter("pixkeys_created_total", 1);
        this.metrics.recordTime("pixkeys_created_latency", aDuration);
        this.metrics.incrementCounter(resolveMetricNameByPixKeyType(aType), 1);

        return CreatePixKeyOutput.from(aPixKey);
    }

    @Generated
    private String resolveMetricNameByPixKeyType(final PixKeyType aType) {
        return switch (aType) {
            case CPF -> "pixkeys_created_by_type_cpf";
            case CNPJ -> "pixkeys_created_by_type_cnpj";
            case EMAIL -> "pixkeys_created_by_type_email";
            case RANDOM -> "pixkeys_created_by_type_random";
            default -> "pixkeys_created_by_type_unexpected";
        };
    }
}
