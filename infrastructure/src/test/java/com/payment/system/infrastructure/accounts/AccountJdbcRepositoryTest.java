package com.payment.system.infrastructure.accounts;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.Account;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AccountJdbcRepositoryTest extends AbstractRepositoryTest {

    @Test
    void testAssertDependencies() {
        Assertions.assertNotNull(accountRepository());
    }

    @Test
    void givenAValidNewAccount_whenCallsSave_thenShouldPersistIt() {
        Assertions.assertEquals(0, countAccounts());

        final var aUserId = "user-123";

        final var aAccount = Account.newAccount(aUserId);

        final var aSavedAccount = this.accountRepository().save(aAccount);

        Assertions.assertEquals(1, countAccounts());

        Assertions.assertEquals(aAccount.getId(), aSavedAccount.getId());
        Assertions.assertEquals(1, aSavedAccount.getVersion());
        Assertions.assertEquals(aUserId, aSavedAccount.getUserId());
        Assertions.assertEquals(aAccount.getBalance(), aSavedAccount.getBalance());
        Assertions.assertEquals(aAccount.getStatus(), aSavedAccount.getStatus());
        Assertions.assertEquals(aAccount.getCreatedAt(), aSavedAccount.getCreatedAt());
        Assertions.assertEquals(aAccount.getUpdatedAt(), aSavedAccount.getUpdatedAt());
        Assertions.assertTrue(aSavedAccount.getClosedAt().isEmpty());
    }
}
