package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.ULID;

import java.util.Objects;

public class DefaultCreatePixKeyUseCase extends CreatePixKeyUseCase {

    private final PixKeyRepository pixKeyRepository;

    public DefaultCreatePixKeyUseCase(
            final PixKeyRepository pixKeyRepository
    ) {
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
    }

    @Override
    public CreatePixKeyOutput execute(final CreatePixKeyCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(CreatePixKeyUseCase.class);
        }

        final var aExistsKey = this.pixKeyRepository.existsByValue(input.value());

        if (aExistsKey) {
            throw DomainException.with("Pix key with value %s already exists".formatted(input.value()));
        }

        final var aType = PixKeyType.from(input.type())
                .orElseThrow(() -> NotFoundException.with("Pix key type %s not found".formatted(input.type())));

        final var aPixKeyValueFactory = new PixKeyValueFactory();

        final var aPixKey = PixKey.newPixKey(
                aPixKeyValueFactory.create(aType, input.value()),
                new AccountId(ULID.fromString(input.accountId()))
        );

        this.pixKeyRepository.save(aPixKey);

        return CreatePixKeyOutput.from(aPixKey);
    }
}
