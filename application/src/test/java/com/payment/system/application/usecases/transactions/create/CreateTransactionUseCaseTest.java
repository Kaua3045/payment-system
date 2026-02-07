package com.payment.system.application.usecases.transactions.create;

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

class CreateTransactionUseCaseTest extends UseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionManager transactionManager;

    @InjectMocks
    private DefaultCreateTransactionUseCase useCase;

    @Test
    void givenAValidCommand_whenExecute_shouldCreateTransaction() {
        final var fromAccount = Account.newAccount("user-1234");
        fromAccount.credit(BigDecimal.TEN);

        final var toAccount = Account.newAccount("user-6789");
        final var pixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                toAccount.getId()
        );

        final var idempotencyKey = "idem-123";

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(fromAccount.getId().value().toString()))
                .thenReturn(Optional.of(fromAccount));

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(pixKey.getKey().value()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(toAccount.getId().value().toString()))
                .thenReturn(Optional.of(toAccount));

        Mockito.when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(returnsFirstArg());

        final var command = CreateTransactionCommand.with(
                fromAccount.getId().value().toString(),
                pixKey.getKey().value(),
                pixKey.getKey().type().name(),
                BigDecimal.TEN,
                idempotencyKey
        );

        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        Assertions.assertNotNull(output);

        Mockito.verify(accountRepository, Mockito.times(2)).accountOfId(any());
        Mockito.verify(pixKeyRepository, Mockito.times(1)).pixKeyOfActiveByValue(any());
        Mockito.verify(transactionRepository, Mockito.times(2)).save(any());
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
        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

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
        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(any()))
                .thenReturn(Optional.empty());

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
    void givenFromAccountInactive_whenExecute_shouldThrowDomainException() {
        final var account = Account.with(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                "user-5678",
                Money.zero(),
                AccountStatus.BLOCKED,
                InstantUtils.now(),
                InstantUtils.now(),
                InstantUtils.now()
        );

        final var expectedErrorMessage = "From account is not active";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(any()))
                .thenReturn(Optional.of(account));

        final var command = CreateTransactionCommand.with(
                "acc",
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(DomainException.class, () -> useCase.execute(command));

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
    void givenToAccountInactive_whenExecute_shouldThrowDomainException() {
        final var from = Account.newAccount("user-1234");
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

        Mockito.when(accountRepository.accountOfId(from.getId().value().toString()))
                .thenReturn(Optional.of(from));

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(any()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(to.getId().value().toString()))
                .thenReturn(Optional.of(to));

        final var command = CreateTransactionCommand.with(
                from.getId().value().toString(),
                "pix",
                "random",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(DomainException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAnInvalidPixKeyType_whenExecute_shouldThrowNotFoundException() {
        final var from = Account.newAccount("user-1234");

        final var expectedErrorMessage = "PixKeyType invalid not found";

        Mockito.when(transactionRepository.existsByIdempotencyKey(any()))
                .thenReturn(false);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(from.getId().value().toString()))
                .thenReturn(Optional.of(from));

        final var command = CreateTransactionCommand.with(
                from.getId().value().toString(),
                "pix",
                "invalid",
                BigDecimal.TEN,
                "idem"
        );

        final var aException = Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenErrorAfterTransactionCreation_whenExecute_shouldMarkTransactionAsFailed() {
        final var fromAccount = Account.newAccount("user-1234");
        fromAccount.credit(BigDecimal.ONE);

        final var toAccount = Account.newAccount("user-5678");

        final var pixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                toAccount.getId()
        );

        final var idempotencyKey = "idem-fail";

        final var expectedErrorMessage = "Insufficient funds";

        final var transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        Mockito.when(transactionManager.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(accountRepository.accountOfId(fromAccount.getId().value().toString()))
                .thenReturn(Optional.of(fromAccount));

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(any()))
                .thenReturn(Optional.of(pixKey));

        Mockito.when(accountRepository.accountOfId(toAccount.getId().value().toString()))
                .thenReturn(Optional.of(toAccount));

        Mockito.when(transactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(returnsFirstArg());

        Mockito.when(transactionRepository.transactionOfIdempotencyKey(idempotencyKey))
                .thenAnswer(inv -> Optional.of(transactionCaptor.getValue()));

        final var command = CreateTransactionCommand.with(
                fromAccount.getId().value().toString(),
                pixKey.getKey().value(),
                pixKey.getKey().type().name(),
                BigDecimal.TEN,
                idempotencyKey
        );

        final var aException = Assertions.assertThrows(DomainException.class, () -> useCase.execute(command));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verify(transactionRepository, Mockito.atLeast(2)).save(any(Transaction.class));
    }
}
