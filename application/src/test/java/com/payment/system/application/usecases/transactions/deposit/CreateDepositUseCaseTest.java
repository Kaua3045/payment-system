package com.payment.system.application.usecases.transactions.deposit;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.exceptions.DomainException;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.valueobjects.Money;
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

class CreateDepositUseCaseTest extends UseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionManager transactionManager;

    @InjectMocks
    private DefaultCreateDepositUseCase useCase;

    @Test
    void givenAValidCommand_whenExecute_shouldCreateDeposit() {
        final var toAccount = Account.newAccount("user-6789");
        final var pixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                toAccount.getId()
        );

        final var aSource = "atm";
        final var idempotencyKey = "idem-123";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(pixKey.getKey().value()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(toAccount.getId().value().toString()))
                .thenReturn(Optional.of(toAccount));

        Mockito.when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(returnsFirstArg());

        final var command = CreateDepositCommand.with(
                pixKey.getKey().value(),
                pixKey.getKey().type().name(),
                aSource,
                BigDecimal.TEN,
                idempotencyKey
        );

        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        Assertions.assertNotNull(output);

        Mockito.verify(accountRepository, Mockito.times(1)).accountOfId(any());
        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfActiveByValue(any());
        Mockito.verify(transactionRepository, Mockito.times(2)).save(any());
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
        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

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
        final var account = Account.newAccount("user-1234");

        final var expectedErrorMessage = "PixKey with value pix was not found";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(any()))
                .thenReturn(Optional.of(account));

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(any()))
                .thenReturn(Optional.empty());

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
        final var to = Account.with(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                "user-5678",
                Money.zero(),
                AccountStatus.BLOCKED,
                InstantUtils.now(),
                InstantUtils.now(),
                InstantUtils.now()
        );
        final var pixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                to.getId()
        );

        final var expectedErrorMessage = "To account is not active";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(any()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(to.getId().value().toString()))
                .thenReturn(Optional.of(to));

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
    void givenAnInvalidPixKeyType_whenExecute_shouldThrowNotFoundException() {
        final var expectedErrorMessage = "PixKeyType invalid not found";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        final var command = CreateDepositCommand.with(
                "pix",
                "invalid",
                "atm",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAnInvalidDepositSOurce_whenExecute_shouldThrowNotFoundException() {
        final var expectedErrorMessage = "DepositSource invalid not found";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

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
        final var toAccount = Account.newAccount("user-5678");

        final var pixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                toAccount.getId()
        );

        final var aSource = "atm";
        final var idempotencyKey = "idem-fail";

        final var transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(any()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(toAccount.getId().value().toString()))
                .thenReturn(Optional.of(toAccount));

        Mockito.when(accountRepository.save(any()))
                .thenThrow(new RuntimeException("teste"));

        Mockito.when(transactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(returnsFirstArg());

        Mockito.when(transactionRepository.transactionOfIdempotencyKey(idempotencyKey))
                .thenAnswer(inv -> Optional.of(transactionCaptor.getValue()));

        final var command = CreateDepositCommand.with(
                pixKey.getKey().value(),
                pixKey.getKey().type().name(),
                aSource,
                BigDecimal.TEN,
                idempotencyKey
        );

        final var aException = Assertions.assertThrows(RuntimeException.class, () -> useCase.execute(command));

        Assertions.assertEquals("teste", aException.getMessage());

        Mockito.verify(transactionRepository, Mockito.atLeast(1)).save(any(Transaction.class));
    }
}
