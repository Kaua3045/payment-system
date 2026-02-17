package com.payment.system.infrastructure.e2e.pixKeys;

import com.payment.system.E2ETest;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.domain.pixkeys.PixKeyStatus;
import com.payment.system.domain.utils.IdentifierUtils;
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

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@E2ETest
@Testcontainers
class PixKeyE2ETest implements MockDsl {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PixKeyRepository pixKeyRepository;

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
    void shouldCreatePixKeyWhenAccountIsActive() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aActualAccount = givenAnAccount(IdentifierUtils.generateNewId());

        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAccountId = aActualAccount.id();

        final var aActualPixKeyCreated = givenAnPixKey(expectedType, expectedValue, expectedAccountId);

        final var aActualPixKey = this.pixKeyRepository.pixKeyOfActiveByValue("11768679037").get();

        Assertions.assertEquals(aActualPixKeyCreated.id(), aActualPixKey.getId().value().toString());
        Assertions.assertEquals(aActualPixKeyCreated.type(), aActualPixKey.getKey().type().name());
        Assertions.assertEquals(aActualPixKeyCreated.accountId(), aActualPixKey.getAccountId().value().toString());
        Assertions.assertEquals(PixKeyStatus.ACTIVE.name(), aActualPixKey.getStatus().name());
        Assertions.assertNotNull(aActualPixKey.getCreatedAt());
        Assertions.assertNotNull(aActualPixKey.getUpdatedAt());
        Assertions.assertTrue(aActualPixKey.getDeletedAt().isEmpty());
    }

    @Test
    void shouldNotCreatePixKeyWhenAccountDoesNotExist() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var expectedType = "cpf";
        final var expectedValue = "117.686.790-37";
        final var expectedAccountId = IdentifierUtils.generateNewMonotonicULID().toString();

        final var expectedErrorMessage = "Account with id %s was not found".formatted(expectedAccountId);

        givenAnPixKeyResult(expectedType, expectedValue, expectedAccountId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(expectedErrorMessage));
    }

    @Test
    void shouldListPixKeysByAccountId() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aActualAccount = givenAnAccount(IdentifierUtils.generateNewId());

        givenAnPixKey("cpf", "117.686.790-37", aActualAccount.id());
        givenAnPixKey("cnpj", "83.112.584/0001-14", aActualAccount.id());
        givenAnPixKey("email", "april.shephard@richestmag.test", aActualAccount.id());
        givenAnPixKey("random", IdentifierUtils.generateNewId(), givenAnAccount(IdentifierUtils.generateNewId()).id());

        listPixKeys(0, 1, Map.of("filters.accountId", aActualAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(0))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(3))
                .andExpect(jsonPath("$.metadata.total_items").value(3))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].value").value("117.686.790-37"))
                .andExpect(jsonPath("$.items[0].type").value("CPF"));

        listPixKeys(1, 1, Map.of("filters.accountId", aActualAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(1))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(3))
                .andExpect(jsonPath("$.metadata.total_items").value(3))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].value").value("83.112.584/0001-14"))
                .andExpect(jsonPath("$.items[0].type").value("CNPJ"));

        listPixKeys(2, 1, Map.of("filters.accountId", aActualAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(2))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(3))
                .andExpect(jsonPath("$.metadata.total_items").value(3))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].value").value("april.shephard@richestmag.test"))
                .andExpect(jsonPath("$.items[0].type").value("EMAIL"));

        listPixKeys(3, 1, Map.of("filters.accountId", aActualAccount.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(3))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(3))
                .andExpect(jsonPath("$.metadata.total_items").value(3))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldFilterPixKeysBySearchTerm() throws Exception {
        Assertions.assertTrue(POSTGRES.isRunning());

        final var aActualAccount = givenAnAccount(IdentifierUtils.generateNewId());

        givenAnPixKey("cpf", "117.686.790-37", aActualAccount.id());
        givenAnPixKey("cnpj", "83.112.584/0001-14", aActualAccount.id());
        givenAnPixKey("email", "april.shephard@richestmag.test", aActualAccount.id());
        givenAnPixKey("random", IdentifierUtils.generateNewId(), givenAnAccount(IdentifierUtils.generateNewId()).id());

        listPixKeys(0, 1, Map.of("filters.accountId", aActualAccount.id(), "filters.type", "cpf", "filters.value", "117.686.790-37"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(0))
                .andExpect(jsonPath("$.metadata.per_page").value(1))
                .andExpect(jsonPath("$.metadata.total_pages").value(1))
                .andExpect(jsonPath("$.metadata.total_items").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].value").value("117.686.790-37"))
                .andExpect(jsonPath("$.items[0].type").value("CPF"));
    }
}
