package com.payment.system.infrastructure.gateways;

import com.payment.system.IntegrationTest;
import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.NotFoundException;
import com.payment.system.domain.utils.IdentifierUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

@IntegrationTest
class AccountJdbcGatewayImplTest {

    @Autowired
    private AccountGateway accountGateway;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void givenAnAccountId_whenAccountOfId_shouldReturnAccount() {
        final var aAccount = Account.newAccount(IdentifierUtils.generateNewMonotonicULID().toString());
        final var aAccountId = aAccount.getId().value().toString();

        Mockito.when(accountRepository.accountOfId(aAccountId))
                .thenReturn(Optional.of(aAccount));

        final var aAccountResponse = this.accountGateway.accountOfId(aAccountId);

        Assertions.assertEquals(aAccount.getId(), aAccountResponse.accountId());
        Assertions.assertEquals(aAccount.getStatus(), aAccountResponse.status());
    }

    @Test
    void givenAnInvalidAccountId_whenAccountOfId_shouldThrowNotFoundException() {
        final var aInvalidAccountId = IdentifierUtils.generateNewMonotonicULID().toString();

        Mockito.when(accountRepository.accountOfId(aInvalidAccountId))
                .thenReturn(Optional.empty());

        final var expectedErrorMessage = "Account with id %s was not found".formatted(aInvalidAccountId);

        final var exception = Assertions.assertThrows(
                NotFoundException.class,
                () -> this.accountGateway.accountOfId(aInvalidAccountId)
        );

        Assertions.assertEquals(expectedErrorMessage, exception.getMessage());
        Assertions.assertEquals("resource_not_found", exception.getErrorCode());
    }
}
