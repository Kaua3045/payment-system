package com.payment.system.application.usecases.pixkeys.retrieve.get;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;

import java.util.Objects;

public class DefaultGetPixKeyByValueUseCase extends GetPixKeyByValueUseCase {

    private final PixKeyRepository pixKeyRepository;

    public DefaultGetPixKeyByValueUseCase(
            final ApplicationLogger aLogger,
            final PixKeyRepository pixKeyRepository
    ) {
        super(aLogger);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
    }

    @Override
    public GetPixKeyByValueOutput execute(final GetPixKeyByValueCommand input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(GetPixKeyByValueUseCase.class);
        }

        final var aType = detectType(input.value());

        final var aKey = new PixKeyValueFactory().create(aType, input.value());

        return this.pixKeyRepository.pixKeyOfValue(aKey.value())
                .map(GetPixKeyByValueOutput::from)
                .orElseThrow(NotFoundException.with(PixKey.class, "value", input.value()));
    }

    private PixKeyType detectType(final String aValue) {
        String aNormalized = aValue.trim();

        if (aNormalized.contains("@")) {
            return PixKeyType.EMAIL;
        }

        String aDigits = aNormalized.replaceAll("\\D", "");

        if (aDigits.length() == 11) {
            return PixKeyType.CPF;
        }

        if (aDigits.length() == 14) {
            return PixKeyType.CNPJ;
        }

        return PixKeyType.RANDOM;
    }
}
