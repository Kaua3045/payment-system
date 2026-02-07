package com.payment.system.infrastructure.pixkeys;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

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
}
