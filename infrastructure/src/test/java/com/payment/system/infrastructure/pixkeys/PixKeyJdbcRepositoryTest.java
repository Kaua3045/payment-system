package com.payment.system.infrastructure.pixkeys;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.pixkeys.*;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.utils.Period;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.time.temporal.ChronoUnit;
import java.util.Map;

class PixKeyJdbcRepositoryTest extends AbstractRepositoryTest {

    @Test
    void testAssertDependencies() {
        Assertions.assertNotNull(pixKeyRepository());
    }

    @Test
    void givenAValidNewPixKey_whenCallsSave_thenShouldPersistIt() {
        Assertions.assertEquals(0, countPixKeys());

        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();

        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.from(aType).get(), aValue), aAccountId);

        final var aSavedPixKey = this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        Assertions.assertEquals(aPixKey.getId(), aSavedPixKey.getId());
        Assertions.assertEquals(1, aSavedPixKey.getVersion());
        Assertions.assertEquals(aType, aSavedPixKey.getKey().type().name());
        Assertions.assertEquals(aValue, aSavedPixKey.getKey().value());
        Assertions.assertEquals(aAccountId, aSavedPixKey.getAccountId());
        Assertions.assertEquals(aPixKey.getStatus(), aSavedPixKey.getStatus());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aSavedPixKey.getCreatedAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aSavedPixKey.getUpdatedAt());
        Assertions.assertTrue(aSavedPixKey.getDeletedAt().isEmpty());
    }

    @Test
    void givenAnExistingPixKeyValue_whenCallsExistsByValue_thenShouldReturnTrue() {
        Assertions.assertEquals(0, countPixKeys());

        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aType = "EMAIL";
        final var aValue = "john.doe@mail.com";

        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.from(aType).get(), aValue), aAccountId);

        this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        final var exists = this.pixKeyRepository().existsByValue(aValue);

        Assertions.assertTrue(exists);
    }

    @Test
    void givenAnNotExistingPixKeyValue_whenCallsExistsByValue_thenShouldReturnFalse() {
        Assertions.assertEquals(0, countPixKeys());

        final var aValue = "7126783186723678";

        final var exists = this.pixKeyRepository().existsByValue(aValue);

        Assertions.assertFalse(exists);
    }

    @Test
    @Sql(statements = {
            "INSERT INTO pix_keys (id, type, key_value, account_id, status, created_at, updated_at, deleted_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', 'INVALID', '12431241241', '01KGB053FZJ0PC00HD9QZWAJ0H', 'ACTIVE', NOW(), NOW(), NULL, 1)"
    })
    void givenAnInvalidPixKeyTypeInDB_whenCallsPixKeyOfActiveByValue_thenShouldReturnIt() {
        Assertions.assertEquals(1, countPixKeys());

        final var expectedErrorMessage = "PixKeyType INVALID not found";

        final var aException = Assertions.assertThrows(NotFoundException.class,
                () -> this.pixKeyRepository().pixKeyOfActiveByValue("12431241241"));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAValidValue_whenCallsPixKeyOfActiveByValue_thenShouldReturnPixKey() {
        Assertions.assertEquals(0, countPixKeys());

        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();

        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.from(aType).get(), aValue), aAccountId);
        this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        final var aSavedPixKey = this.pixKeyRepository().pixKeyOfActiveByValue(aPixKey.getKey().value()).get();

        Assertions.assertEquals(aPixKey.getId(), aSavedPixKey.getId());
        Assertions.assertEquals(1, aSavedPixKey.getVersion());
        Assertions.assertEquals(aType, aSavedPixKey.getKey().type().name());
        Assertions.assertEquals(aValue, aSavedPixKey.getKey().value());
        Assertions.assertEquals(aAccountId, aSavedPixKey.getAccountId());
        Assertions.assertEquals(aPixKey.getStatus(), aSavedPixKey.getStatus());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aSavedPixKey.getCreatedAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aSavedPixKey.getUpdatedAt());
        Assertions.assertTrue(aSavedPixKey.getDeletedAt().isEmpty());
    }

    @Test
    void givenAnInvalidValue_whenCallsPixKeyOfActiveByValue_thenShouldReturnEmpty() {
        Assertions.assertEquals(0, countPixKeys());

        final var aSavedPixKey = this.pixKeyRepository().pixKeyOfActiveByValue("7912378816823681");

        Assertions.assertTrue(aSavedPixKey.isEmpty());
    }

    @Test
    void givenNoPixKeys_whenCallsListAll_thenShouldReturnEmptyPagination() {
        Assertions.assertEquals(0, countPixKeys());

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc"
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());

        final var metadata = result.metadata();
        Assertions.assertEquals(0, metadata.currentPage());
        Assertions.assertEquals(10, metadata.perPage());
        Assertions.assertEquals(0, metadata.totalItems());
        Assertions.assertEquals(0, metadata.totalPages());
    }

    @Test
    void givenMultiplePixKeys_whenCallsListAllWithPagination_thenShouldPaginateCorrectly() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        for (int i = 0; i < 15; i++) {
            final var pixKey = PixKey.newPixKey(
                    new PixKeyValueFactory().create(
                            PixKeyType.RANDOM,
                            "key-" + i
                    ),
                    accountId
            );
            this.pixKeyRepository().save(pixKey);
        }

        Assertions.assertEquals(15, countPixKeys());

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc"
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(10, result.items().size());

        final var metadata = result.metadata();
        Assertions.assertEquals(0, metadata.currentPage());
        Assertions.assertEquals(10, metadata.perPage());
        Assertions.assertEquals(15, metadata.totalItems());
        Assertions.assertEquals(2, metadata.totalPages());
    }

    @Test
    void givenMultiplePixKeys_whenCallsSecondPage_thenShouldReturnRemainingItems() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        for (int i = 0; i < 12; i++) {
            this.pixKeyRepository().save(
                    PixKey.newPixKey(
                            new PixKeyValueFactory().create(
                                    PixKeyType.RANDOM,
                                    "pix-" + i
                            ),
                            accountId
                    )
            );
        }

        final var query = SearchQuery.newSearchQuery(
                1,
                10,
                null,
                "createdAt",
                "asc"
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(2, result.items().size());
        Assertions.assertEquals(12, result.metadata().totalItems());
        Assertions.assertEquals(2, result.metadata().totalPages());
    }

    @Test
    void givenPixKeys_whenCallsListAllWithTerms_thenShouldFilterByValueOrType() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "john@mail.com"),
                        accountId
                )
        );

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.RANDOM, "random-key"),
                        accountId
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of(
                        "type", "EMAIL",
                        "value", "john@mail.com"
                )
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
        Assertions.assertEquals("john@mail.com", result.items().get(0).getKey().value());
    }

    @Test
    void givenValueFilterWithoutType_whenCallsListAll_thenShouldThrowNotFoundException() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "john@mail.com"),
                        accountId
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of(
                        "value", "john@mail.com"
                )
        );

        final var exception = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.pixKeyRepository().listAll(query)
        );

        Assertions.assertEquals(
                "PixKeyType required on use value to search",
                exception.getMessage()
        );
    }

    @Test
    void givenPixKeys_whenCallsListAllWithTerms_thenShouldFilterByTypeUsingILike() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "a@mail.com"),
                        accountId
                )
        );

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.RANDOM, "abc"),
                        accountId
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                "EMAIL",
                "createdAt",
                "asc"
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
        Assertions.assertEquals(PixKeyType.EMAIL, result.items().get(0).getKey().type());
    }

    @Test
    void givenPixKeys_whenSortByUpdatedAt_thenShouldOrderCorrectly() throws InterruptedException {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var first = this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "a@mail.com"),
                        accountId
                )
        );

        final var second = this.pixKeyRepository().save(
                PixKey.with(
                        new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                        0L,
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "b@mail.com"),
                        accountId,
                        PixKeyStatus.ACTIVE,
                        InstantUtils.now().plus(10, ChronoUnit.MINUTES),
                        InstantUtils.now().plus(10, ChronoUnit.MINUTES),
                        null
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "updatedAt",
                "asc"
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(first.getId(), result.items().get(0).getId());
        Assertions.assertEquals(second.getId(), result.items().get(1).getId());
    }

    @Test
    void givenPixKeys_whenCallsListAllWithTypeFilter_thenShouldReturnOnlyThatType() {
        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.EMAIL, "a@mail.com"),
                        accountId
                )
        );

        this.pixKeyRepository().save(
                PixKey.newPixKey(
                        new PixKeyValueFactory().create(PixKeyType.RANDOM, "abc"),
                        accountId
                )
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                Map.of("type", "EMAIL")
        );

        final var result = this.pixKeyRepository().listAll(query);

        Assertions.assertEquals(1, result.items().size());
        Assertions.assertEquals(PixKeyType.EMAIL, result.items().get(0).getKey().type());
    }

    @Test
    void givenPixKeysSavedViaRepository_whenFilterByPeriod_thenShouldReturnOnlyInsidePeriod() {
        final var repository = this.pixKeyRepository();

        final var factory = new PixKeyValueFactory();

        final var accountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var oldDate = InstantUtils.now().minus(10, ChronoUnit.DAYS);
        final var newDate = InstantUtils.now();

        final var oldPixKey = PixKey.with(
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                factory.create(PixKeyType.from("EMAIL").get(), "old@mail.com"),
                accountId,
                PixKeyStatus.ACTIVE,
                oldDate,
                oldDate,
                null
        );

        final var newPixKey = PixKey.with(
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                factory.create(PixKeyType.from("EMAIL").get(), "new@mail.com"),
                accountId,
                PixKeyStatus.ACTIVE,
                newDate,
                newDate,
                null
        );

        repository.save(oldPixKey);
        repository.save(newPixKey);

        final var period = new Period(
                InstantUtils.now().minus(2, ChronoUnit.DAYS),
                InstantUtils.now().plus(1, ChronoUnit.DAYS)
        );

        final var query = SearchQuery.newSearchQuery(
                0,
                10,
                null,
                "createdAt",
                "asc",
                period
        );

        final var result = repository.listAll(query);

        Assertions.assertEquals(1, result.items().size());

        final var pixKey = result.items().get(0);

        Assertions.assertEquals("new@mail.com", pixKey.getKey().value());
        Assertions.assertEquals(PixKeyStatus.ACTIVE, pixKey.getStatus());
    }

    @Test
    void givenAValidPixKeyValue_whenCallsPixKeyOfValue_thenReturnPixKey() {
        Assertions.assertEquals(0, countPixKeys());

        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aType = "RANDOM";
        final var aValue = IdentifierUtils.generateNewId();

        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.from(aType).get(), aValue), aAccountId);
        this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        final var aSavedPixKey = this.pixKeyRepository().pixKeyOfValue(aPixKey.getKey().value()).get();

        Assertions.assertEquals(aPixKey.getId(), aSavedPixKey.getId());
        Assertions.assertEquals(1, aSavedPixKey.getVersion());
        Assertions.assertEquals(aType, aSavedPixKey.getKey().type().name());
        Assertions.assertEquals(aValue, aSavedPixKey.getKey().value());
        Assertions.assertEquals(aAccountId, aSavedPixKey.getAccountId());
        Assertions.assertEquals(aPixKey.getStatus(), aSavedPixKey.getStatus());
        Assertions.assertEquals(aPixKey.getCreatedAt(), aSavedPixKey.getCreatedAt());
        Assertions.assertEquals(aPixKey.getUpdatedAt(), aSavedPixKey.getUpdatedAt());
        Assertions.assertTrue(aSavedPixKey.getDeletedAt().isEmpty());
    }

    @Test
    void givenAnInvalidPixKeyValue_whenCallsPixKeyOfValue_thenReturnEmpty() {
        Assertions.assertEquals(0, countPixKeys());

        final var aValue = IdentifierUtils.generateNewId();

        Assertions.assertEquals(0, countPixKeys());

        final var aPixKey = this.pixKeyRepository().pixKeyOfValue(aValue);

        Assertions.assertTrue(aPixKey.isEmpty());
    }
}
