package com.payment.system.application.usecases.accounts.retrieve.id;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;

class GetAccountByIdUseCaseTest extends UseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DefaultGetAccountByIdUseCase useCase;

    @Test
    void givenAValidId_whenCallsGetAccountById_shouldReturnAccount() {
        final var aAccount = Account.newAccount("user-123");

        final var aCommand = GetAccountByIdCommand.with(aAccount.getId().value().toString());

        Mockito.when(accountRepository.accountOfId(Mockito.any()))
                .thenReturn(Optional.of(aAccount));

        final var aAccountOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aAccountOutput);
        Assertions.assertEquals(aAccount.getId().value().toString(), aAccountOutput.id());
        Assertions.assertEquals(aAccount.getUserId(), aAccountOutput.userId());
        Assertions.assertEquals(aAccount.getStatus().name(), aAccountOutput.status());
        Assertions.assertEquals(aAccount.getBalance().amount(), aAccountOutput.balance());
        Assertions.assertEquals(aAccount.getCreatedAt(), aAccountOutput.createdAt());
        Assertions.assertEquals(aAccount.getUpdatedAt(), aAccountOutput.updatedAt());
        Assertions.assertNull(aAccountOutput.closedAt());

        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(
                argThat(id -> Objects.equals(id, aAccount.getId().value().toString()))
        );
    }

    @Test
    void givenAnInvalidId_whenCallsGetAccountById_shouldReturnNotFoundException() {
        final var aCommand = GetAccountByIdCommand.with("invalid-id");

        final var expectedErrorMessage = "Account with id invalid-id was not found";

        Mockito.when(accountRepository.accountOfId(Mockito.any()))
                .thenReturn(Optional.empty());

        final var actualException = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.useCase.execute(aCommand)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(
                argThat(id -> Objects.equals(id, "invalid-id"))
        );
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsGetAccountById_shouldThrowException() {
        final var expectedErrorMessage = "Input to GetAccountByIdUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(accountRepository, Mockito.never()).accountOfId(Mockito.any());
    }
}
