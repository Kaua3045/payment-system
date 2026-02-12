package com.payment.system.application.usecases.transactions.retrieve.list;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.PaginationMetadata;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.DepositSource;
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
import java.util.List;

class ListTransactionsUseCaseTest extends UseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DefaultListTransactionsUseCase useCase;

    @Test
    void givenAnInvalidNullInput_whenCallListTransactionsUseCase_thenThrowUseCaseInputCannotBeNullException() {
        final var expectedErrorMessage = "Input to ListTransactionsUseCase cannot be null";

        final var aException = Assertions.assertThrows(UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void givenAValidInput_whenCallListTransactionsUseCase_thenReturnPagination() {
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var aTransactionOne = Transaction.newTransaction(
                aAccountId,
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal("10.50")),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                IdentifierUtils.generateNewId()
        );

        final var aTransactionTwo = Transaction.newTransaction(
                aAccountId,
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(new BigDecimal("15.50")),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                IdentifierUtils.generateNewId()
        );

        final var aPixKeys = List.of(aTransactionOne, aTransactionTwo);

        final var aPage = 0;
        final var aPerPage = 10;
        final var aTotalPages = 1;
        final var aTerms = "";
        final var aSort = "createdAt";
        final var aDirection = "asc";

        final var aSearchQuery = SearchQuery.newSearchQuery(aPage, aPerPage, aTerms, aSort, aDirection);

        final var aMetadata = new PaginationMetadata(aPage, aPerPage, aTotalPages, aPixKeys.size());
        final var aPagination = new Pagination<>(aMetadata, aPixKeys);

        final var aItemsCount = 2;
        final var aResult = aPagination.map(ListTransactionsOutput::from);

        Mockito.when(transactionRepository.listAll(aSearchQuery)).thenReturn(aPagination);

        final var aOutput = this.useCase.execute(aSearchQuery);

        Assertions.assertEquals(aItemsCount, aOutput.metadata().totalItems());
        Assertions.assertEquals(aResult.items(), aOutput.items());
        Assertions.assertEquals(aResult.metadata(), aOutput.metadata());

        Mockito.verify(transactionRepository, Mockito.times(1)).listAll(aSearchQuery);
    }

    @Test
    void givenAValidInputButHasNoData_whenCallListTransactionsUseCase_thenReturnEmptyData() {
        final var aPage = 0;
        final var aPerPage = 10;
        final var aTotalPages = 1;
        final var aTerms = "";
        final var aSort = "createdAt";
        final var aDirection = "asc";

        final var aSearchQuery = SearchQuery.newSearchQuery(aPage, aPerPage, aTerms, aSort, aDirection);

        final var aMetadata = new PaginationMetadata(aPage, aPerPage, aTotalPages, 0);
        final var aPagination = new Pagination<Transaction>(aMetadata, List.of());

        final var aItemsCount = 0;

        Mockito.when(transactionRepository.listAll(aSearchQuery))
                .thenReturn(aPagination);

        final var aOutput = this.useCase.execute(aSearchQuery);

        Assertions.assertEquals(aItemsCount, aOutput.metadata().totalItems());
        Assertions.assertTrue(aOutput.items().isEmpty());
        Assertions.assertEquals(aMetadata, aOutput.metadata());

        Mockito.verify(transactionRepository, Mockito.times(1)).listAll(aSearchQuery);
    }
}
