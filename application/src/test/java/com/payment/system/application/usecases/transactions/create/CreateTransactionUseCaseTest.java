package com.payment.system.application.usecases.transactions.create;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.PixKeyIsNotActiveException;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class CreateTransactionUseCaseTest extends UseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PixKeyGateway pixKeyGateway;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionManager transactionManager;

    @InjectMocks
    private DefaultCreateTransactionUseCase useCase;

    @Test
    void givenAValidCommand_whenExecute_shouldCreateTransaction() {
        final var fromAccount = Account.newAccount("user-1234");
        final var toAccount = Account.newAccount("user-6789");
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var idempotencyKey = "idem-123";
        final var amount = BigDecimal.TEN;

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, toAccount.getId().value().toString()));

        Mockito.when(accountGateway.accountOfId(fromAccount.getId().value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, fromAccount.getId()));

        Mockito.when(accountGateway.accountOfId(toAccount.getId().value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, toAccount.getId()));

        Mockito.when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(returnsFirstArg());

        final var command = CreateTransactionCommand.with(
                fromAccount.getId().value().toString(),
                "pix-key-value",
                PixKeyType.RANDOM.name(),
                amount,
                idempotencyKey
        );

        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        Assertions.assertNotNull(output);

        Mockito.verify(accountGateway, Mockito.times(2)).accountOfId(anyString());
        Mockito.verify(pixKeyGateway, Mockito.times(1)).pixKeyOfActiveByValue(anyString(), anyString());
        Mockito.verify(ledgerRepository, Mockito.times(1)).createReservation(any());
        Mockito.verify(ledgerRepository, Mockito.times(1)).saveAll(any());
        Mockito.verify(transactionRepository, Mockito.times(1)).save(any());
        Mockito.verify(ledgerRepository, Mockito.times(1)).confirmReservation(any());
    }

    @Test
    void givenNullCommand_whenExecute_shouldThrowException() {
        final var expectedErrorMessage = "Input to CreateTransactionUseCase cannot be null";

        final var aException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void givenAmountLessThanOrEqualZero_whenExecute_shouldThrowException() {
        final var expectedErrorMessage = "Amount must be greater than zero";

        final var command = CreateTransactionCommand.with(
                "acc",
                "pix",
                "random",
                BigDecimal.ZERO,
                "idem"
        );

        final var aException = Assertions.assertThrows(
                DomainException.class,
                () -> useCase.execute(command)
        );

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenFromAccountNotFound_whenExecute_shouldThrowNotFoundException() {
        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, new PixKeyId(IdentifierUtils.generateNewMonotonicULID()), "to-acc"));

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountGateway.accountOfId("acc"))
                .thenThrow(NotFoundException.with(Account.class, "acc").get());

        final var expectedErrorMessage = "Account with id acc was not found";

        final var command = CreateTransactionCommand.with(
                "acc",
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenFromAccountInactive_whenExecute_shouldThrowAccountIsNotActiveException() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, new PixKeyId(IdentifierUtils.generateNewMonotonicULID()), "to-acc"));

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountGateway.accountOfId(accountId.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.BLOCKED, accountId));

        final var command = CreateTransactionCommand.with(
                accountId.value().toString(),
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        Assertions.assertThrows(com.payment.system.application.exceptions.AccountIsNotActiveException.class, () -> useCase.execute(command));
    }

    @Test
    void givenPixKeyNotFound_whenExecute_shouldThrowNotFoundException() {
        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenThrow(NotFoundException.with(PixKey.class, "value", "pix").get());

        final var command = CreateTransactionCommand.with(
                "acc",
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals("PixKey with value pix was not found", aException.getMessage());
    }

    @Test
    void givenToAccountInactive_whenExecute_shouldThrowAccountIsNotActiveException() {
        final var from = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var to = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, to.value().toString()));

        Mockito.when(accountGateway.accountOfId(from.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, from));

        Mockito.when(accountGateway.accountOfId(to.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.BLOCKED, to));

        final var command = CreateTransactionCommand.with(
                from.value().toString(),
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        Assertions.assertThrows(com.payment.system.application.exceptions.AccountIsNotActiveException.class, () -> useCase.execute(command));
    }

    @Test
    void givenInsufficientFunds_whenExecute_shouldThrowInsufficientFundsException() {
        final var from = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var to = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, to.value().toString()));

        Mockito.when(accountGateway.accountOfId(from.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, from));

        Mockito.when(accountGateway.accountOfId(to.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, to));

        Mockito.doThrow(new InsufficientFundsException())
                .when(ledgerRepository).createReservation(any());

        final var command = CreateTransactionCommand.with(
                from.value().toString(),
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        Assertions.assertThrows(InsufficientFundsException.class, () -> useCase.execute(command));
    }

    @Test
    void givenAnInactivePixKey_whenExecute_shouldThrowPixKeyIsNotActiveException() {
        final var from = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var to = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(false, pixKeyId, to.value().toString()));

        final var command = CreateTransactionCommand.with(
                from.value().toString(),
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        Assertions.assertThrows(PixKeyIsNotActiveException.class, () -> useCase.execute(command));
    }
}
