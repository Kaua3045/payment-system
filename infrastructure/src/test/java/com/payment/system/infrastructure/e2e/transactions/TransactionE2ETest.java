package com.payment.system.infrastructure.e2e.transactions;

import com.payment.system.E2ETest;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.TransactionStatus;
import com.payment.system.domain.transactions.TransactionType;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@E2ETest
@Testcontainers
class TransactionE2ETest implements MockDsl {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TransactionRepository transactionRepository;

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
    void shouldPersistTransactionWithCompletedStatus_whenTransferIsValid() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aFromAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aToAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aFromAccountPixKeyValue = IdentifierUtils.generateNewId();

        final var expectedFromAccountId = aFromAccount.id();
        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAmount = new Money(new BigDecimal("10.50")).amount();

        final var aActualPixKeyId = givenAnPixKey(expectedType, expectedValue, aToAccount.id()).id();
        givenAnPixKey("random", aFromAccountPixKeyValue, aFromAccount.id());
        givenAnDeposit(aFromAccountPixKeyValue, "random", "external", new BigDecimal("12.50"));

        final var aActualTransactionId = givenAnTransaction(expectedFromAccountId, expectedValue, expectedType, expectedAmount).transactionId();

        final var aActualTransaction = this.transactionRepository.transactionOfIdAndAccountId(aActualTransactionId, expectedFromAccountId).get();

        Assertions.assertEquals(aFromAccount.id(), aActualTransaction.getFromAccountId().value().toString());
        Assertions.assertEquals(aToAccount.id(), aActualTransaction.getToAccountId().value().toString());
        Assertions.assertEquals(aActualPixKeyId, aActualTransaction.getPixKeyId().value().toString());
        Assertions.assertEquals(expectedAmount, aActualTransaction.getAmount().amount());
        Assertions.assertEquals(TransactionType.TRANSFER, aActualTransaction.getType());
        Assertions.assertEquals(TransactionStatus.COMPLETED, aActualTransaction.getStatus());
        Assertions.assertEquals(DepositSource.EXTERNAL, aActualTransaction.getSource());
        Assertions.assertNotNull(aActualTransaction.getCreatedAt());
        Assertions.assertNotNull(aActualTransaction.getUpdatedAt());
    }

    @Test
    void shouldPersistDepositTransactionWithCompletedStatus_whenDepositIsValid() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aToAccount = givenAnAccount(IdentifierUtils.generateNewId());

        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedSource = "external";
        final var expectedAmount = new Money(new BigDecimal("10.50")).amount();

        final var aActualPixKeyId = givenAnPixKey(expectedType, expectedValue, aToAccount.id());

        final var aActualTransactionId = givenAnDeposit(expectedValue, expectedType, expectedSource, expectedAmount).transactionId();

        final var aActualTransaction = this.transactionRepository.transactionOfIdAndAccountId(aActualTransactionId, aToAccount.id()).get();

        Assertions.assertEquals(AccountId.system().value().toString(), aActualTransaction.getFromAccountId().value().toString());
        Assertions.assertEquals(aToAccount.id(), aActualTransaction.getToAccountId().value().toString());
        Assertions.assertEquals(aActualPixKeyId.id(), aActualTransaction.getPixKeyId().value().toString());
        Assertions.assertEquals(expectedAmount, aActualTransaction.getAmount().amount());
        Assertions.assertEquals(TransactionType.TRANSFER, aActualTransaction.getType());
        Assertions.assertEquals(TransactionStatus.COMPLETED, aActualTransaction.getStatus());
        Assertions.assertEquals(DepositSource.EXTERNAL, aActualTransaction.getSource());
        Assertions.assertNotNull(aActualTransaction.getCreatedAt());
        Assertions.assertNotNull(aActualTransaction.getUpdatedAt());
    }

    @Test
    void shouldReturnTransactionDetails_whenTransactionExistsForAccount() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aFromAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aToAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aFromAccountPixKeyValue = IdentifierUtils.generateNewId();

        final var expectedFromAccountId = aFromAccount.id();
        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAmount = new Money(new BigDecimal("10.50")).amount();

        final var aActualPixKeyId = givenAnPixKey(expectedType, expectedValue, aToAccount.id()).id();
        givenAnPixKey("random", aFromAccountPixKeyValue, aFromAccount.id());
        givenAnDeposit(aFromAccountPixKeyValue, "random", "external", new BigDecimal("12.50"));

        final var aActualTransactionId = givenAnTransaction(expectedFromAccountId, expectedValue, expectedType, expectedAmount).transactionId();

        final var aActualTransaction = retrieveAnTransaction(aFromAccount.id(), aActualTransactionId);

        Assertions.assertEquals(aFromAccount.id(), aActualTransaction.fromAccountId());
        Assertions.assertEquals(aToAccount.id(), aActualTransaction.toAccountId());
        Assertions.assertEquals(aActualPixKeyId, aActualTransaction.pixKeyId());
        Assertions.assertEquals(expectedAmount, aActualTransaction.amount());
        Assertions.assertEquals(TransactionType.TRANSFER.name(), aActualTransaction.type());
        Assertions.assertEquals(TransactionStatus.COMPLETED.name(), aActualTransaction.status());
        Assertions.assertEquals(DepositSource.EXTERNAL.name(), aActualTransaction.source());
        Assertions.assertNotNull(aActualTransaction.createdAt());
        Assertions.assertNotNull(aActualTransaction.updatedAt());
        Assertions.assertNotNull(aActualTransaction.idempotencyKey());
        Assertions.assertNull(aActualTransaction.failureReason());
    }

    @Test
    void shouldReturnPaginatedTransactions_whenListingByAccountId() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aFromAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aToAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aFromAccountPixKeyValue = IdentifierUtils.generateNewId();

        final var expectedFromAccountId = aFromAccount.id();
        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAmount = new Money(new BigDecimal("10.50")).amount();

        final var aActualPixKeyId = givenAnPixKey(expectedType, expectedValue, aToAccount.id()).id();
        final var aActualPixKeyIdDeposit = givenAnPixKey("random", aFromAccountPixKeyValue, aFromAccount.id()).id();
        final var aTransactionIdDeposit = givenAnDeposit(aFromAccountPixKeyValue, "random", "external", new BigDecimal("12.50")).transactionId();

        final var aActualTransactionId = givenAnTransaction(expectedFromAccountId, expectedValue, expectedType, expectedAmount).transactionId();

        listTransactions(0, 1, Map.of("filters.accountId", aFromAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(0))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(2))
                .andExpect(jsonPath("$.metadata.total_items").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].transaction_id").value(aTransactionIdDeposit))
                .andExpect(jsonPath("$.items[0].pix_key_id").value(aActualPixKeyIdDeposit));

        listTransactions(1, 1, Map.of("filters.accountId", aFromAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(1))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(2))
                .andExpect(jsonPath("$.metadata.total_items").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].transaction_id").value(aActualTransactionId))
                .andExpect(jsonPath("$.items[0].pix_key_id").value(aActualPixKeyId));

        listTransactions(2, 1, Map.of("filters.accountId", aFromAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(2))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(2))
                .andExpect(jsonPath("$.metadata.total_items").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldFilterTransactionsBySearchTerm() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aFromAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aToAccount = givenAnAccount(IdentifierUtils.generateNewId());
        final var aFromAccountPixKeyValue = IdentifierUtils.generateNewId();

        final var expectedFromAccountId = aFromAccount.id();
        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAmount = new Money(new BigDecimal("10.50")).amount();

        final var aActualPixKeyId = givenAnPixKey(expectedType, expectedValue, aToAccount.id()).id();
        givenAnPixKey("random", aFromAccountPixKeyValue, aFromAccount.id());
        givenAnDeposit(aFromAccountPixKeyValue, "random", "external", new BigDecimal("12.50"));

        final var aActualTransactionId = givenAnTransaction(expectedFromAccountId, expectedValue, expectedType, expectedAmount).transactionId();

        listTransactions(0, 1, "", "createdAt", "desc", null, null, Map.of("filters.accountId", aFromAccount.id(), "filters.type", "transfer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(0))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(2))
                .andExpect(jsonPath("$.metadata.total_items").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].transaction_id").value(aActualTransactionId))
                .andExpect(jsonPath("$.items[0].pix_key_id").value(aActualPixKeyId));
    }
}
