package com.payment.system.domain.accounts;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.handler.NotificationHandler;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest extends UnitTest {

    @Test
    void givenAValidParams_whenCallsNewAccount_thenInstantiateIt() {
        final var aUserId = "user-123";

        final var aAccount = Account.newAccount(aUserId);

        assertNotNull(aAccount.getId());
        assertEquals(aUserId, aAccount.getUserId());
        assertEquals(0L, aAccount.getVersion());
        assertEquals(Money.zero(), aAccount.getBalance());
        assertEquals(AccountStatus.ACTIVE, aAccount.getStatus());
        assertNotNull(aAccount.getCreatedAt());
        assertNotNull(aAccount.getUpdatedAt());
        assertTrue(aAccount.getClosedAt().isEmpty());
        Assertions.assertDoesNotThrow(() -> aAccount.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidValues_whenCallsAccountWith_thenInstantiateIt() {
        final var aAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var aVersion = 2L;
        final var aUserId = "user-123";
        final var aBalance = new Money(BigDecimal.valueOf(1000.0));
        final var aStatus = AccountStatus.BLOCKED;
        final var aCreatedAt = InstantUtils.now();
        final var aUpdatedAt = InstantUtils.now();
        final var aClosedAt = InstantUtils.now();

        final var aAccount = Account.with(
                aAccountId,
                aVersion,
                aUserId,
                aBalance,
                aStatus,
                aCreatedAt,
                aUpdatedAt,
                aClosedAt
        );

        assertEquals(aAccountId, aAccount.getId());
        assertEquals(aVersion, aAccount.getVersion());
        assertEquals(aUserId, aAccount.getUserId());
        assertEquals(aBalance, aAccount.getBalance());
        assertEquals(aStatus, aAccount.getStatus());
        assertEquals(aCreatedAt, aAccount.getCreatedAt());
        assertEquals(aUpdatedAt, aAccount.getUpdatedAt());
        assertEquals(aClosedAt, aAccount.getClosedAt().get());
        Assertions.assertDoesNotThrow(() -> aAccount.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidName_whenCallsAccountStatusFrom_thenReturnIt() {
        final var aExpected = AccountStatus.ACTIVE;

        final var aActual = AccountStatus.from("ACTIVE").get();

        assertEquals(aExpected, aActual);
    }

    @Test
    void givenAnInvalidName_whenCallsAccountStatusFrom_thenReturnEmpty() {
        final var aActual = AccountStatus.from("INVALID_STATUS");

        assertFalse(aActual.isPresent());
    }

    @Test
    void testCallToStringInAccount() {
        final var aUserId = "user-123";
        final var aAccount = Account.newAccount(aUserId);

        final var toStringResult = aAccount.toString();

        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("Account"));
        assertTrue(toStringResult.contains(aUserId));
    }
}
