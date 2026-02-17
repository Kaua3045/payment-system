package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.Account;
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
    private final AccountRepository accountRepository;

    public DefaultCreatePixKeyUseCase(
            final PixKeyRepository pixKeyRepository,
            final AccountRepository accountRepository
    ) {
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
        this.accountRepository = Objects.requireNonNull(accountRepository);
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

        final var aAccount = this.accountRepository.accountOfId(input.accountId())
                .orElseThrow(NotFoundException.with(Account.class, input.accountId()));

        final var aPixKeyValueFactory = new PixKeyValueFactory();

        final var aPixKey = PixKey.newPixKey(
                aPixKeyValueFactory.create(aType, input.value()),
                aAccount.getId()
        );

        this.pixKeyRepository.save(aPixKey);

        return CreatePixKeyOutput.from(aPixKey);
    }
}
