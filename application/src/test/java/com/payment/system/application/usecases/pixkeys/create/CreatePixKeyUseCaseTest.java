package com.payment.system.application.usecases.pixkeys.create;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Objects;
import java.util.Optional;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.argThat;

class CreatePixKeyUseCaseTest extends UseCaseTest {

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DefaultCreatePixKeyUseCase useCase;

    @Test
    void givenAValidCommand_whenCallsCreatePixKey_shouldReturnPixKeyId() {
        final var aAccount = Account.newAccount(IdentifierUtils.generateNewId());

        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();
        final var anAccountId = aAccount.getId();

        final var aCommand = CreatePixKeyCommand.with(
                aType,
                aValue,
                anAccountId.value().toString()
        );

        Mockito.when(pixKeyRepository.existsByValue(aValue))
                .thenReturn(false);
        Mockito.when(accountRepository.accountOfId(Mockito.any()))
                .thenReturn(Optional.of(aAccount));
        Mockito.when(pixKeyRepository.save(Mockito.any()))
                .thenAnswer(returnsFirstArg());

        final var aPixKeyOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aPixKeyOutput);
        Assertions.assertNotNull(aPixKeyOutput.id());
        Assertions.assertEquals(aType, aPixKeyOutput.type());
        Assertions.assertEquals(anAccountId.value().toString(), aPixKeyOutput.accountId());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).existsByValue(aValue);
        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(Mockito.any());
        Mockito.verify(pixKeyRepository, Mockito.times(1)).save(argThat(aCmd ->
                Objects.equals(aCmd.getKey().type().name(), aType) &&
                        Objects.equals(aCmd.getKey().value(), aValue) &&
                        Objects.equals(aCmd.getAccountId().value().toString(), anAccountId.value().toString())
        ));
    }

    @Test
    void givenAnExistingPixKeyValue_whenCallsCreatePixKey_shouldReturnDomainException() {
        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();
        final var anAccountId = IdentifierUtils.generateNewMonotonicULID();

        final var expectedErrorMessage = "Pix key with value %s already exists".formatted(aValue);

        final var aCommand = CreatePixKeyCommand.with(
                aType,
                aValue,
                anAccountId.toString()
        );

        Mockito.when(pixKeyRepository.existsByValue(aValue))
                .thenReturn(true);

        final var actualException = Assertions.assertThrows(
                Exception.class,
                () -> this.useCase.execute(aCommand)
        );

        Assertions.assertEquals(
                expectedErrorMessage,
                actualException.getMessage()
        );

        Mockito.verify(pixKeyRepository, Mockito.times(1)).existsByValue(aValue);
        Mockito.verify(pixKeyRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenAnInvalidPixKeyType_whenCallsCreatePixKey_shouldReturnNotFoundException() {
        final var aType = "INVALID_TYPE";
        final var aValue = IdentifierUtils.generateNewId();
        final var anAccountId = IdentifierUtils.generateNewMonotonicULID();

        final var expectedErrorMessage = "Pix key type %s not found".formatted(aType);

        final var aCommand = CreatePixKeyCommand.with(
                aType,
                aValue,
                anAccountId.toString()
        );

        Mockito.when(pixKeyRepository.existsByValue(aValue))
                .thenReturn(false);

        final var actualException = Assertions.assertThrows(
                Exception.class,
                () -> this.useCase.execute(aCommand)
        );

        Assertions.assertEquals(
                expectedErrorMessage,
                actualException.getMessage()
        );

        Mockito.verify(pixKeyRepository, Mockito.times(1)).existsByValue(aValue);
        Mockito.verify(pixKeyRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenAnInvalidAccountId_whenCallsCreatePixKey_shouldReturnNotFoundException() {
        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();
        final var anAccountId = IdentifierUtils.generateNewMonotonicULID();

        final var expectedErrorMessage = "Account with id %s was not found".formatted(anAccountId);

        final var aCommand = CreatePixKeyCommand.with(
                aType,
                aValue,
                anAccountId.toString()
        );

        Mockito.when(pixKeyRepository.existsByValue(aValue))
                .thenReturn(false);
        Mockito.when(accountRepository.accountOfId(Mockito.any()))
                .thenReturn(Optional.empty());

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> this.useCase.execute(aCommand));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).existsByValue(aValue);
        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(Mockito.any());
        Mockito.verify(pixKeyRepository, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsCreatePixKey_shouldThrowException() {
        final var expectedErrorMessage = "Input to CreatePixKeyUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(pixKeyRepository, Mockito.never()).existsByValue(Mockito.any());
        Mockito.verify(pixKeyRepository, Mockito.never()).save(Mockito.any());
    }
}
