package com.payment.system.domain.pixkeys;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.domain.validation.handler.NotificationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PixKeyTest extends UnitTest {

    @Test
    void givenAValidParams_whenCallsNewPixKey_thenInstantiateACorrectObject() {
        final var expectedType = PixKeyType.EMAIL;
        final var expectedValue = "john.doe@test.com";
        final var expectedAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var aActualPixKey = PixKey.newPixKey(
                expectedType,
                expectedValue,
                expectedAccountId
        );

        Assertions.assertNotNull(aActualPixKey);
        Assertions.assertNotNull(aActualPixKey.getId());
        Assertions.assertEquals(0L, aActualPixKey.getVersion());
        Assertions.assertEquals(expectedType, aActualPixKey.getType());
        Assertions.assertEquals(expectedValue, aActualPixKey.getValue());
        Assertions.assertEquals(expectedAccountId, aActualPixKey.getAccountId());
        Assertions.assertEquals(PixKeyStatus.ACTIVE, aActualPixKey.getStatus());
        Assertions.assertNotNull(aActualPixKey.getCreatedAt());
        Assertions.assertNotNull(aActualPixKey.getUpdatedAt());
        Assertions.assertTrue(aActualPixKey.getDeletedAt().isEmpty());
        Assertions.assertDoesNotThrow(() -> aActualPixKey.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidValues_whenCallsWith_thenReturnANewObjectWithValues() {
        final var expectedPixKeyId = new PixKeyId(IdentifierUtils.generateNewMonotonicULID());
        final var expectedVersion = 1L;
        final var expectedType = PixKeyType.EMAIL;
        final var expectedValue = "john.doe@test.com";
        final var expectedAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());
        final var expectedStatus = PixKeyStatus.DELETED;
        final var expectedCreatedAt = InstantUtils.now();
        final var expectedUpdatedAt = InstantUtils.now();
        final var expectedDeletedAt = InstantUtils.now();

        final var aActualPixKey = PixKey.with(
                expectedPixKeyId,
                expectedVersion,
                expectedType,
                expectedValue,
                expectedAccountId,
                expectedStatus,
                expectedCreatedAt,
                expectedUpdatedAt,
                expectedDeletedAt
        );

        Assertions.assertNotNull(aActualPixKey);
        Assertions.assertEquals(expectedPixKeyId, aActualPixKey.getId());
        Assertions.assertEquals(expectedVersion, aActualPixKey.getVersion());
        Assertions.assertEquals(expectedType, aActualPixKey.getType());
        Assertions.assertEquals(expectedValue, aActualPixKey.getValue());
        Assertions.assertEquals(expectedAccountId, aActualPixKey.getAccountId());
        Assertions.assertEquals(expectedStatus, aActualPixKey.getStatus());
        Assertions.assertEquals(expectedCreatedAt, aActualPixKey.getCreatedAt());
        Assertions.assertEquals(expectedUpdatedAt, aActualPixKey.getUpdatedAt());
        Assertions.assertTrue(aActualPixKey.getDeletedAt().isPresent());
        Assertions.assertEquals(expectedDeletedAt, aActualPixKey.getDeletedAt().get());
        Assertions.assertDoesNotThrow(() -> aActualPixKey.validate(NotificationHandler.create()));
    }

    @Test
    void givenAValidValue_whenCallsPixKeyStatusFrom_thenReturnTheCorrectEnum() {
        Assertions.assertEquals(PixKeyStatus.ACTIVE, PixKeyStatus.from("ACTIVE").get());
    }

    @Test
    void givenAnInvalidValue_whenCallsPixKeyStatusFrom_thenReturnAnEmptyOptional() {
        Assertions.assertTrue(PixKeyStatus.from("INVALID").isEmpty());
    }

    @Test
    void givenAValidValue_whenCallsPixKeyTypeFrom_thenReturnTheCorrectEnum() {
        Assertions.assertEquals(PixKeyType.EMAIL, PixKeyType.from("EMAIL").get());
    }

    @Test
    void givenAnInvalidValue_whenCallsPixKeyTypeFrom_thenReturnAnEmptyOptional() {
        Assertions.assertTrue(PixKeyType.from("INVALID").isEmpty());
    }

    @Test
    void testCallToStringInPixKey() {
        final var expectedType = PixKeyType.EMAIL;
        final var expectedValue = "john.doe@test.com";
        final var expectedAccountId = new AccountId(IdentifierUtils.generateNewMonotonicULID());

        final var aActualPixKey = PixKey.newPixKey(
                expectedType,
                expectedValue,
                expectedAccountId
        );

        final var toString = aActualPixKey.toString();

        Assertions.assertNotNull(toString);
        Assertions.assertTrue(toString.contains(PixKey.class.getSimpleName()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getId().value().toString()));
        Assertions.assertTrue(toString.contains(String.valueOf(aActualPixKey.getVersion())));
        Assertions.assertTrue(toString.contains(aActualPixKey.getType().toString()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getValue()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getAccountId().value().toString()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getStatus().toString()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getCreatedAt().toString()));
        Assertions.assertTrue(toString.contains(aActualPixKey.getUpdatedAt().toString()));
    }
}
