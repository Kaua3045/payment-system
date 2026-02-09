package com.payment.system.application.usecases.pixkeys.retrieve.list;

import com.payment.system.application.UseCaseTest;
import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.PaginationMetadata;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;

class ListPixKeysUseCaseTest extends UseCaseTest {

    @Mock
    private PixKeyRepository pixKeyRepository;

    @InjectMocks
    private DefaultListPixKeysUseCase useCase;

    @Test
    void givenAnInvalidNullInput_whenCallListPixKeysUseCase_thenThrowUseCaseInputCannotBeNullException() {
        final var expectedErrorMessage = "Input to ListPixKeysUseCase cannot be null";

        final var aException = Assertions.assertThrows(UseCaseInputCannotBeNullException.class,
                () -> this.useCase.execute(null));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());

        Mockito.verifyNoInteractions(pixKeyRepository);
    }

    @Test
    void givenAValidInput_whenCallListPixKeysUseCase_thenReturnPagination() {
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var aPixKeyOne = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                aAccountId
        );

        final var aPixKeyTwo = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                aAccountId
        );

        final var aPixKeys = List.of(aPixKeyOne, aPixKeyTwo);

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
        final var aResult = aPagination.map(ListPixKeysOutput::from);

        Mockito.when(pixKeyRepository.listAll(aSearchQuery)).thenReturn(aPagination);

        final var aOutput = this.useCase.execute(aSearchQuery);

        Assertions.assertEquals(aItemsCount, aOutput.metadata().totalItems());
        Assertions.assertEquals(aResult.items(), aOutput.items());
        Assertions.assertEquals(aResult.metadata(), aOutput.metadata());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).listAll(aSearchQuery);
    }

    @Test
    void givenAValidInputButHasNoData_whenCallListPixKeysUseCase_thenReturnEmptyData() {
        final var aPage = 0;
        final var aPerPage = 10;
        final var aTotalPages = 1;
        final var aTerms = "";
        final var aSort = "createdAt";
        final var aDirection = "asc";

        final var aSearchQuery = SearchQuery.newSearchQuery(aPage, aPerPage, aTerms, aSort, aDirection);

        final var aMetadata = new PaginationMetadata(aPage, aPerPage, aTotalPages, 0);
        final var aPagination = new Pagination<PixKey>(aMetadata, List.of());

        final var aItemsCount = 0;

        Mockito.when(pixKeyRepository.listAll(aSearchQuery))
                .thenReturn(aPagination);

        final var aOutput = this.useCase.execute(aSearchQuery);

        Assertions.assertEquals(aItemsCount, aOutput.metadata().totalItems());
        Assertions.assertTrue(aOutput.items().isEmpty());
        Assertions.assertEquals(aMetadata, aOutput.metadata());

        Mockito.verify(pixKeyRepository, Mockito.times(1)).listAll(aSearchQuery);
    }
}
