package com.payment.system.infrastructure.e2e.accounts;

import com.payment.system.E2ETest;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.AccountStatus;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.e2e.MockDsl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@E2ETest
@Testcontainers
class AccountE2ETest implements MockDsl {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AccountRepository accountRepository;

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("payment-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("postgres.port", () -> POSTGRES.getMappedPort(5432));
    }

    @Override
    public MockMvc mvc() {
        return mvc;
    }

    @Test
    void shouldOpenNewBankAccountWithInitialBalanceZero() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var expectedUserId = IdentifierUtils.generateNewId();

        final var aActualAccountId = givenAnAccount(expectedUserId).id();

        final var aActualAccount = this.accountRepository.accountOfId(aActualAccountId).get();

        Assertions.assertEquals(expectedUserId, aActualAccount.getUserId());
        Assertions.assertEquals(Money.zero().amount(), aActualAccount.getBalance().amount());
        Assertions.assertEquals(AccountStatus.ACTIVE.name(), aActualAccount.getStatus().name());
        Assertions.assertNotNull(aActualAccount.getCreatedAt());
        Assertions.assertNotNull(aActualAccount.getUpdatedAt());
        Assertions.assertTrue(aActualAccount.getClosedAt().isEmpty());
    }

    @Test
    void shouldRetrieveAccountWithAccountId() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var expectedUserId = IdentifierUtils.generateNewId();

        final var aActualAccountId = givenAnAccount(expectedUserId).id();

        final var aActualAccount = retrieveAnAccount(aActualAccountId);

        Assertions.assertNotNull(aActualAccount.id());
        Assertions.assertEquals(expectedUserId, aActualAccount.userId());
        Assertions.assertEquals(Money.zero().amount(), aActualAccount.balance());
        Assertions.assertEquals(AccountStatus.ACTIVE.name(), aActualAccount.status());
        Assertions.assertNotNull(aActualAccount.createdAt());
        Assertions.assertNotNull(aActualAccount.updatedAt());
        Assertions.assertNull(aActualAccount.closedAt());
    }
}
