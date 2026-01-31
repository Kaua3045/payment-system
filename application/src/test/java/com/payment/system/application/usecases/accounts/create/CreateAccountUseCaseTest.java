package com.payment.system.application.usecases.accounts.create;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.AccountStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Objects;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.argThat;

class CreateAccountUseCaseTest extends UseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DefaultCreateAccountUseCase useCase;

    @Test
    void givenAValidCommand_whenCallsCreateAccount_shouldReturnAccountId() {
        final var aUserId = "user-123";

        final var aCommand = CreateAccountCommand.with(aUserId);

        Mockito.when(accountRepository.save(Mockito.any()))
                .thenAnswer(returnsFirstArg());

        final var aAccountOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aAccountOutput);
        Assertions.assertNotNull(aAccountOutput.id());
        Assertions.assertEquals(aUserId, aAccountOutput.userId());
        Assertions.assertNotNull(aAccountOutput.status());

        Mockito.verify(accountRepository, Mockito.times(1)).save(argThat(aCmd ->
                Objects.equals(aCmd.getUserId(), aUserId) &&
                        Objects.equals(aCmd.getStatus().name(), AccountStatus.ACTIVE.name())));
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsCreateAccount_shouldThrowException() {
        final var expectedErrorMessage = "Input to CreateAccountUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any());
    }
}
