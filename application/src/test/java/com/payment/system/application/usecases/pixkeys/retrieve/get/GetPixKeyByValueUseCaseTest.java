package com.payment.system.application.usecases.pixkeys.retrieve.get;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;

class GetPixKeyByValueUseCaseTest extends UseCaseTest {

    @Mock
    private PixKeyRepository pixKeyRepository;

    @InjectMocks
    private DefaultGetPixKeyByValueUseCase useCase;

    @Test
    void givenAValidValueRandom_whenCallsGetPixKeyByValue_shouldReturnPixKey() {
        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()), new AccountId(IdentifierUtils.generateNewMonotonicULID()));

        final var aCommand = GetPixKeyByValueCommand.with(aPixKey.getKey().value());

        Mockito.when(pixKeyRepository.pixKeyOfValue(Mockito.any()))
                .thenReturn(Optional.of(aPixKey));

        final var aPixKeyOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aPixKeyOutput);
        Assertions.assertEquals(aPixKey.getId().value().toString(), aPixKeyOutput.id());
        Assertions.assertEquals(aPixKey.getKey().type().name(), aPixKeyOutput.type());
        Assertions.assertEquals(aPixKey.getKey().value(), aPixKeyOutput.value());
        Assertions.assertEquals(aPixKey.getAccountId().value().toString(), aPixKeyOutput.accountId());
        Assertions.assertEquals(aPixKey.getStatus().name(), aPixKeyOutput.status());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aPixKeyOutput.createdAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aPixKeyOutput.updatedAt());
        Assertions.assertNull(aPixKeyOutput.deletedAt());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfValue(
                argThat(value -> Objects.equals(value, aPixKey.getKey().value()))
        );
    }

    @Test
    void givenAValidValueEmail_whenCallsGetPixKeyByValue_shouldReturnPixKey() {
        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.EMAIL, "john.doe@gmail.com"), new AccountId(IdentifierUtils.generateNewMonotonicULID()));

        final var aCommand = GetPixKeyByValueCommand.with(aPixKey.getKey().value());

        Mockito.when(pixKeyRepository.pixKeyOfValue(Mockito.any()))
                .thenReturn(Optional.of(aPixKey));

        final var aPixKeyOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aPixKeyOutput);
        Assertions.assertEquals(aPixKey.getId().value().toString(), aPixKeyOutput.id());
        Assertions.assertEquals(aPixKey.getKey().type().name(), aPixKeyOutput.type());
        Assertions.assertEquals(aPixKey.getKey().value(), aPixKeyOutput.value());
        Assertions.assertEquals(aPixKey.getAccountId().value().toString(), aPixKeyOutput.accountId());
        Assertions.assertEquals(aPixKey.getStatus().name(), aPixKeyOutput.status());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aPixKeyOutput.createdAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aPixKeyOutput.updatedAt());
        Assertions.assertNull(aPixKeyOutput.deletedAt());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfValue(
                argThat(value -> Objects.equals(value, aPixKey.getKey().value()))
        );
    }

    @Test
    void givenAValidValueCpf_whenCallsGetPixKeyByValue_shouldReturnPixKey() {
        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.CPF, "852.332.270-10"), new AccountId(IdentifierUtils.generateNewMonotonicULID()));

        final var aCommand = GetPixKeyByValueCommand.with(aPixKey.getKey().value());

        Mockito.when(pixKeyRepository.pixKeyOfValue(Mockito.any()))
                .thenReturn(Optional.of(aPixKey));

        final var aPixKeyOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aPixKeyOutput);
        Assertions.assertEquals(aPixKey.getId().value().toString(), aPixKeyOutput.id());
        Assertions.assertEquals(aPixKey.getKey().type().name(), aPixKeyOutput.type());
        Assertions.assertEquals(aPixKey.getKey().value(), aPixKeyOutput.value());
        Assertions.assertEquals(aPixKey.getAccountId().value().toString(), aPixKeyOutput.accountId());
        Assertions.assertEquals(aPixKey.getStatus().name(), aPixKeyOutput.status());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aPixKeyOutput.createdAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aPixKeyOutput.updatedAt());
        Assertions.assertNull(aPixKeyOutput.deletedAt());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfValue(
                argThat(value -> Objects.equals(value, aPixKey.getKey().value()))
        );
    }

    @Test
    void givenAValidValueCnpj_whenCallsGetPixKeyByValue_shouldReturnPixKey() {
        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.CNPJ, "12.345.678/0001-95"), new AccountId(IdentifierUtils.generateNewMonotonicULID()));

        final var aCommand = GetPixKeyByValueCommand.with(aPixKey.getKey().value());

        Mockito.when(pixKeyRepository.pixKeyOfValue(Mockito.any()))
                .thenReturn(Optional.of(aPixKey));

        final var aPixKeyOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aPixKeyOutput);
        Assertions.assertEquals(aPixKey.getId().value().toString(), aPixKeyOutput.id());
        Assertions.assertEquals(aPixKey.getKey().type().name(), aPixKeyOutput.type());
        Assertions.assertEquals(aPixKey.getKey().value(), aPixKeyOutput.value());
        Assertions.assertEquals(aPixKey.getAccountId().value().toString(), aPixKeyOutput.accountId());
        Assertions.assertEquals(aPixKey.getStatus().name(), aPixKeyOutput.status());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aPixKeyOutput.createdAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aPixKeyOutput.updatedAt());
        Assertions.assertNull(aPixKeyOutput.deletedAt());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfValue(
                argThat(value -> Objects.equals(value, aPixKey.getKey().value()))
        );
    }

    @Test
    void givenAnInvalidId_whenCallsGetPixKeyByValue_shouldReturnNotFoundException() {
        final var aCommand = GetPixKeyByValueCommand.with("invalid-value");

        final var expectedErrorMessage = "PixKey with value invalid-value was not found";

        Mockito.when(pixKeyRepository.pixKeyOfValue(Mockito.any()))
                .thenReturn(Optional.empty());

        final var actualException = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.useCase.execute(aCommand)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfValue(
                argThat(value -> Objects.equals(value, "invalid-value"))
        );
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsGetPixKeyByValue_shouldThrowException() {
        final var expectedErrorMessage = "Input to GetPixKeyByValueUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(pixKeyRepository, Mockito.never()).pixKeyOfValue(Mockito.any());
    }
}
