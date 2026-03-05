package com.payment.system.infrastructure.accounts;

import com.payment.system.AbstractRepositoryTest;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.exceptions.ConflictException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

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

    @Test
    void givenAValidUpdatedAccount_whenCallsSave_thenShouldUpdateIt() {
        Assertions.assertEquals(0, countAccounts());

        final var aUserId = "user-123";

        final var aAccount = Account.newAccount(aUserId);

        final var aSavedAccount = this.accountRepository().save(aAccount);

        Assertions.assertEquals(1, countAccounts());

        aAccount.credit(BigDecimal.TEN);

        this.accountRepository().save(aAccount);

        Assertions.assertEquals(aAccount.getId(), aSavedAccount.getId());
        Assertions.assertEquals(2, aSavedAccount.getVersion());
        Assertions.assertEquals(aUserId, aSavedAccount.getUserId());
        Assertions.assertEquals(aAccount.getBalance(), aSavedAccount.getBalance());
        Assertions.assertEquals(aAccount.getStatus(), aSavedAccount.getStatus());
        Assertions.assertEquals(aAccount.getCreatedAt(), aSavedAccount.getCreatedAt());
        Assertions.assertEquals(aAccount.getUpdatedAt(), aSavedAccount.getUpdatedAt());
        Assertions.assertTrue(aSavedAccount.getClosedAt().isEmpty());
    }

    @Test
    void givenAValidAccountId_whenCallsAccountOfId_thenShouldReturnIt() {
        Assertions.assertEquals(0, countAccounts());

        final var aUserId = "user-123";

        final var aAccount = Account.newAccount(aUserId);

        final var aSavedAccount = this.accountRepository().save(aAccount);

        Assertions.assertEquals(1, countAccounts());

        final var aFoundAccount = this.accountRepository().accountOfId(aSavedAccount.getId().value().toString()).get();

        Assertions.assertEquals(aSavedAccount.getId(), aFoundAccount.getId());
        Assertions.assertEquals(aSavedAccount.getVersion(), aFoundAccount.getVersion());
        Assertions.assertEquals(aSavedAccount.getUserId(), aFoundAccount.getUserId());
        Assertions.assertEquals(aSavedAccount.getBalance().amount(), aFoundAccount.getBalance().amount());
        Assertions.assertEquals(aSavedAccount.getStatus(), aFoundAccount.getStatus());
        Assertions.assertEquals(aSavedAccount.getCreatedAt(), aFoundAccount.getCreatedAt());
        Assertions.assertEquals(aSavedAccount.getUpdatedAt(), aFoundAccount.getUpdatedAt());
        Assertions.assertEquals(aSavedAccount.getClosedAt(), aFoundAccount.getClosedAt());
    }

    @Test
    @Sql(statements = {
            "INSERT INTO accounts (id, user_id, balance, status, created_at, updated_at, closed_at, version) " +
                    "VALUES ('01KGB053FZJ0PC00HD9QZWAJ0H', 'user-123', 1000, 'INVALID', NOW(), NOW(), NULL, 1)"
    })
    void givenAnInvalidAccountStatusInDB_whenCallsAccountOfId_thenShouldReturnIt() {
        Assertions.assertEquals(1, countAccounts());

        final var expectedErrorMessage = "should not be null";
        final var expectedErrorProperty = "status";

        final var aException = Assertions.assertThrows(ValidationException.class,
                () -> this.accountRepository().accountOfId("01KGB053FZJ0PC00HD9QZWAJ0H"));

        Assertions.assertEquals(1, aException.getErrors().size());
        Assertions.assertEquals(expectedErrorMessage, aException.getErrors().getFirst().message());
        Assertions.assertEquals(expectedErrorProperty, aException.getErrors().getFirst().property());
    }

    @Test
    void givenAValidExistsAccount_whenCallSaveButVersionIsNotMatch_thenThrowsConflictException() {
        Assertions.assertEquals(0, countAccounts());

        final var aUserId = "user-123";

        final var aAccount = Account.newAccount(aUserId);

        final var aSavedAccount = this.accountRepository().save(aAccount);

        final var expectedErrorMessage = "Account with identifier %s and version 2 does not match, account was updated by another transaction"
                .formatted(aAccount.getId().value());

        Assertions.assertEquals(1, countAccounts());

        aSavedAccount.credit(BigDecimal.TEN);
        aSavedAccount.incrementVersion(); // Simulate version mismatch

        final var aAccountRepositoryVariable = this.accountRepository(); // Variable to use in lambda, because this.customerRepository() is not allowed in lambda
        // this is a way to test if the exception is thrown, THIS IS A SCAM, in future disable this rule in sonar
        final var aException = Assertions.assertThrows(ConflictException.class,
                () -> aAccountRepositoryVariable.save(aAccount));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }
}
