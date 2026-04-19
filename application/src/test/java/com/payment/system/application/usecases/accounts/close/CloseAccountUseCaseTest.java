package com.payment.system.application.usecases.accounts.close;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Objects;
import java.util.Optional;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.argThat;

class CloseAccountUseCaseTest extends UseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DefaultCloseAccountUseCase useCase;

    @Test
    void givenAValidCommand_whenCallsCloseAccount_shouldReturnNothing() {
        final var aAccount = Account.newAccount("user-12345");

        final var aAccountId = aAccount.getId().value().toString();

        final var aCommand = CloseAccountCommand.with(aAccountId);

        Mockito.when(accountRepository.accountOfId(aAccountId))
                .thenReturn(Optional.of(aAccount));
        Mockito.when(accountRepository.save(Mockito.any()))
                .thenAnswer(returnsFirstArg());

        Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(argThat(aCmd ->
                Objects.equals(aCmd, aAccountId)));
        Mockito.verify(accountRepository, Mockito.times(1)).save(argThat(aCmd ->
                Objects.equals(aCmd.getId().value().toString(), aAccountId) &&
                        Objects.equals(aCmd.getStatus().name(), AccountStatus.CLOSED.name())));
    }

    @Test
    void givenAnInvalidAccountId_whenCallsCloseAccount_shouldThrowsNotFoundException() {
        final var aAccountId = "27831723871";

        final var expectedErrorMessage = "Account with id 27831723871 was not found";

        final var aCommand = CloseAccountCommand.with(aAccountId);

        Mockito.when(accountRepository.accountOfId(aAccountId))
                .thenReturn(Optional.empty());

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> this.useCase.execute(aCommand));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(argThat(aCmd ->
                Objects.equals(aCmd, aAccountId)));
        Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsCloseAccount_shouldThrowException() {
        final var expectedErrorMessage = "Input to CloseAccountUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any());
    }
}
