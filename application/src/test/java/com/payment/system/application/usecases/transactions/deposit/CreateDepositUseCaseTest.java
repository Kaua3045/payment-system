package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.PixKeyIsNotActiveException;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.ConflictException;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class CreateDepositUseCaseTest extends UseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PixKeyGateway pixKeyGateway;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionManager transactionManager;

    @InjectMocks
    private DefaultCreateDepositUseCase useCase;

    @Test
    void givenAValidCommand_whenExecute_shouldCreateDeposit() {
        final var toAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyValue = "pix-key-value";

        final var aSource = "atm";
        final var idempotencyKey = "idem-123";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, toAccountId.value().toString()));

        Mockito.when(accountGateway.accountOfId(toAccountId.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, toAccountId));

        Mockito.when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(returnsFirstArg());

        final var command = CreateDepositCommand.with(
                pixKeyValue,
                PixKeyType.RANDOM.name(),
                aSource,
                BigDecimal.TEN,
                idempotencyKey
        );

        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        Assertions.assertNotNull(output);

        Mockito.verify(accountGateway, Mockito.times(1)).accountOfId(anyString());
        Mockito.verify(pixKeyGateway, Mockito.times(1)).pixKeyOfActiveByValue(anyString(), anyString());
        Mockito.verify(transactionRepository, Mockito.times(1)).save(any());
        Mockito.verify(ledgerRepository, Mockito.times(1)).save(any());
    }

    @Test
    void givenNullCommand_whenExecute_shouldThrowException() {
        final var expectedErrorMessage = "Input to CreateDepositUseCase cannot be null";

        final var aException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void givenAmountLessThanOrEqualZero_whenExecute_shouldThrowException() {
        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        final var expectedErrorMessage = "Amount must be greater than zero";

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
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
    void givenPixKeyNotFound_whenExecute_shouldThrowNotFoundException() {
        final var expectedErrorMessage = "PixKey with value pix was not found";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenThrow(NotFoundException.with(PixKey.class, "value", "pix").get());

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenToAccountInactive_whenExecute_shouldThrowDomainException() {
        final var toAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());

        final var expectedErrorMessage = "Account To is not active";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, toAccountId.value().toString()));

        Mockito.when(accountGateway.accountOfId(toAccountId.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.BLOCKED, toAccountId));

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(DomainException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAnInvalidDepositSource_whenExecute_shouldThrowNotFoundException() {
        final var expectedErrorMessage = "DepositSource invalid not found";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "invalid",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenErrorAfterTransactionCreation_whenExecute_shouldMarkTransactionAsFailed() {
        final var toAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var idempotencyKey = "idem-fail";

        final var transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, toAccountId.value().toString()));

        Mockito.when(accountGateway.accountOfId(toAccountId.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, toAccountId));

        Mockito.doThrow(new RuntimeException("teste"))
                .when(ledgerRepository).save(any());

        Mockito.when(transactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(returnsFirstArg());

        Mockito.when(transactionRepository.transactionOfIdempotencyKey(idempotencyKey))
                .thenAnswer(inv -> Optional.of(transactionCaptor.getValue()));

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
                BigDecimal.TEN,
                idempotencyKey
        );

        final var aException = Assertions.assertThrows(RuntimeException.class, () -> useCase.execute(command));

        Assertions.assertEquals("teste", aException.getMessage());

        Mockito.verify(transactionRepository, Mockito.atLeast(2)).save(any(Transaction.class));
        Assertions.assertEquals("FAILED", transactionCaptor.getValue().getStatus().name());
    }

    @Test
    void givenAnConflictingVersion_whenExecute_shouldThrowConflictException() {
        final var toAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var idempotencyKey = "idem-conflict";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(true, pixKeyId, toAccountId.value().toString()));

        Mockito.when(accountGateway.accountOfId(toAccountId.value().toString()))
                .thenReturn(new AccountGateway.AccountResponse(AccountStatus.ACTIVE, toAccountId));

        Mockito.when(transactionRepository.save(any()))
                .thenThrow(ConflictException.with("Version conflict"));

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
                BigDecimal.TEN,
                idempotencyKey
        );

        final var aException = Assertions.assertThrows(ConflictException.class, () -> useCase.execute(command));

        Assertions.assertEquals("Version conflict", aException.getMessage());

        Mockito.verify(transactionRepository, Mockito.times(1)).save(any());
    }

    @Test
    void givenAnInactivePixKey_whenExecute_shouldThrowPixKeyIsNotActiveException() {
        final var toAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var pixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var idempotencyKey = "idem-inactive";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyGateway.pixKeyOfActiveByValue(anyString(), anyString()))
                .thenReturn(new PixKeyGateway.PixKeyActiveResponse(false, pixKeyId, toAccountId.value().toString()));

        final var command = CreateDepositCommand.with(
                "pix",
                "random",
                "atm",
                BigDecimal.TEN,
                idempotencyKey
        );

        final var aException = Assertions.assertThrows(PixKeyIsNotActiveException.class, () -> useCase.execute(command));

        Assertions.assertEquals("PixKey pix is not active", aException.getMessage());

        Mockito.verify(transactionRepository, Mockito.times(0)).save(any());
    }
}
