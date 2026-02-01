package com.payment.system.infrastructure.pixkeys;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        final var aPixKey = PixKey.newPixKey(PixKeyType.from(aType).get(), aValue, aAccountId);

        final var aSavedPixKey = this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        Assertions.assertEquals(aPixKey.getId(), aSavedPixKey.getId());
        Assertions.assertEquals(1, aSavedPixKey.getVersion());
        Assertions.assertEquals(aType, aSavedPixKey.getType().name());
        Assertions.assertEquals(aValue, aSavedPixKey.getValue());
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

        final var aPixKey = PixKey.newPixKey(PixKeyType.from(aType).get(), aValue, aAccountId);

        this.pixKeyRepository().save(aPixKey);

        Assertions.assertEquals(1, countPixKeys());

        final var exists = this.pixKeyRepository().existsByValue(aValue);

        Assertions.assertTrue(exists);
    }
}
