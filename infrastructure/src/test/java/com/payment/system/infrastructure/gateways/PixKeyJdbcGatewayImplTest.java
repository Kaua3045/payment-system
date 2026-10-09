package com.payment.system.infrastructure.gateways;

import com.payment.system.IntegrationTest;
import com.payment.system.application.exceptions.PixKeyIsNotActiveException;
import com.payment.system.application.gateways.PixKeyGateway;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.pixkeys.*;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

@IntegrationTest
class PixKeyJdbcGatewayImplTest {

    @Autowired
    private PixKeyGateway pixKeyGateway;

    @MockitoBean
    private PixKeyRepository pixKeyRepository;

    @Test
    void givenAnPixKey_whenPixKeyOfActiveByValue_shouldReturnPixKeyActiveResponse() {
        final var aPixKeyType = "EMAIL";
        final var aPixKeyValue = "john.doe@hotmail.com";
        final var aPixKey = PixKey.newPixKey(
                new PixKeyValueFactory().create(PixKeyType.from(aPixKeyType).get(), aPixKeyValue),
                new AccountId(IdentifierUtils.generateNewMonotonicULID())
        );

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(aPixKeyValue))
                .thenReturn(Optional.of(aPixKey));

        final var aPixKeyResponse = this.pixKeyGateway.pixKeyOfActiveByValue(aPixKeyType, aPixKeyValue);

        Assertions.assertTrue(aPixKeyResponse.active());
        Assertions.assertEquals(aPixKey.getId(), aPixKeyResponse.pixKeyId());
        Assertions.assertEquals(aPixKey.getAccountId().value().toString(), aPixKeyResponse.accountId());
    }

    @Test
    void givenAnInactivePixKey_whenPixKeyOfActiveByValue_shouldThrowPixKeyIsNotActiveException() {
        final var aPixKeyType = "EMAIL";
        final var aPixKeyValue = "john.doe@gmail.com";
        final var aPixKey = PixKey.with(
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                0L,
                new PixKeyValueFactory().create(PixKeyType.from(aPixKeyType).get(), aPixKeyValue),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                PixKeyStatus.INACTIVE,
                InstantUtils.now(),
                InstantUtils.now(),
                InstantUtils.now()
        );

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(aPixKeyValue))
                .thenReturn(Optional.of(aPixKey));

        final var expectedErrorMessage = "PixKey %s is not active".formatted(aPixKeyValue);

        final var aException = Assertions.assertThrows(
                PixKeyIsNotActiveException.class,
                () -> this.pixKeyGateway.pixKeyOfActiveByValue(aPixKeyType, aPixKeyValue)
        );

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
        Assertions.assertEquals("pix_key_inactive", aException.getErrorCode());
    }

    @Test
    void givenANonExistentPixKey_whenPixKeyOfActiveByValue_shouldThrowNotFoundException() {
        final var aPixKeyValue = "NON-exists@gmail.com";

        Mockito.when(pixKeyRepository.pixKeyOfActiveByValue(aPixKeyValue))
                .thenReturn(Optional.empty());

        final var expectedErrorMessage = "PixKey with value %s was not found".formatted(aPixKeyValue);

        final var aException = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.pixKeyGateway.pixKeyOfActiveByValue("EMAIL", aPixKeyValue)
        );

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
        Assertions.assertEquals("resource_not_found", aException.getErrorCode());
    }
}
