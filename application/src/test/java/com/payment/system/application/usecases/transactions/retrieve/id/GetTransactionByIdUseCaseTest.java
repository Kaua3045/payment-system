package com.payment.system.application.usecases.transactions.retrieve.id;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

class GetTransactionByIdUseCaseTest extends UseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DefaultGetTransactionByIdUseCase useCase;

    @Test
    void givenAValidIds_whenCallsGetTransactionById_shouldReturnTransaction() {
        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                "1238712712678368126834"
        );

        final var aCommand = GetTransactionByIdCommand.with(aTransaction.getId().value().toString(), aTransaction.getFromAccountId().value().toString());

        Mockito.when(transactionRepository.transactionOfIdAndAccountId(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(aTransaction));

        final var aTransactionOutput = Assertions.assertDoesNotThrow(() -> this.useCase.execute(aCommand));

        Assertions.assertNotNull(aTransactionOutput);
        Assertions.assertEquals(aTransaction.getId().value().toString(), aTransactionOutput.transactionId());
        Assertions.assertEquals(aTransaction.getVersion(), aTransactionOutput.version());
        Assertions.assertEquals(aTransaction.getFromAccountId().value().toString(), aTransactionOutput.fromAccountId());
        Assertions.assertEquals(aTransaction.getToAccountId().value().toString(), aTransactionOutput.toAccountId());
        Assertions.assertEquals(aTransaction.getPixKeyId().value().toString(), aTransactionOutput.pixKeyId());
        Assertions.assertEquals(aTransaction.getAmount().amount(), aTransactionOutput.amount());
        Assertions.assertEquals(aTransaction.getStatus().name(), aTransactionOutput.status());
        Assertions.assertEquals(aTransaction.getType().name(), aTransactionOutput.type());
        Assertions.assertEquals(aTransaction.getIdempotencyKey(), aTransactionOutput.idempotencyKey());
        Assertions.assertTrue(aTransaction.getFailureReason().isEmpty());
        Assertions.assertEquals(aTransaction.getCreatedAt(), aTransactionOutput.createdAt());
        Assertions.assertEquals(aTransaction.getUpdatedAt(), aTransactionOutput.updatedAt());


        Mockito.verify(transactionRepository, Mockito.times(1)).transactionOfIdAndAccountId(Mockito.any(), Mockito.any());
    }

    @Test
    void givenAnInvalidIds_whenCallsGetTransactionById_shouldReturnNotFoundException() {
        final var aCommand = GetTransactionByIdCommand.with("invalid-id", "invalid");

        final var expectedErrorMessage = "Transaction with id invalid-id was not found";

        Mockito.when(transactionRepository.transactionOfIdAndAccountId(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());

        final var actualException = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.useCase.execute(aCommand)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(transactionRepository, Mockito.times(1)).transactionOfIdAndAccountId(Mockito.any(), Mockito.any());
    }

    @Test
    void givenAnInvalidNullCommand_whenCallsGetTransactionById_shouldThrowException() {
        final var expectedErrorMessage = "Input to GetTransactionByIdUseCase cannot be null";

        final var actualException = Assertions.assertThrows(
                UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null)
        );

        Assertions.assertEquals(expectedErrorMessage, actualException.getMessage());

        Mockito.verify(transactionRepository, Mockito.never()).transactionOfIdAndAccountId(Mockito.any(), Mockito.any());
    }
}
