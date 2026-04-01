package com.payment.system.infrastructure.rest;

import com.payment.system.ApiTest;
import com.payment.system.ControllerTest;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyCommand;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyOutput;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.get.GetPixKeyByValueOutput;
import com.payment.system.application.usecases.pixkeys.retrieve.get.GetPixKeyByValueUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysOutput;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysUseCase;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.PaginationMetadata;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.utils.InstantUtils;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.pixkeys.res.ListPixKeysResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(controllers = PixKeyAPI.class)
class PixKeyAPITest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreatePixKeyUseCase createPixKeyUseCase;

    @MockitoBean
    private ListPixKeysUseCase listPixKeysUseCase;

    @MockitoBean
    private GetPixKeyByValueUseCase getPixKeyByValueUseCase;

    @Captor
    private ArgumentCaptor<CreatePixKeyCommand> createPixKeyCommandCaptor;

    @Test
    void givenAValidRequest_whenCallsCreatePixKey_shouldReturnHttp201() throws Exception {
        final var aAccountId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var aType = "CPF";
        final var aValue = "12345678900";

        final var expectedPixKeyId = IdentifierUtils.generateNewMonotonicULID().toString();

        final var aRequestBody = """
                {
                    "account_id": "%s",
                    "type": "%s",
                    "value": "%s"
                }
                """.formatted(aAccountId, aType, aValue);

        Mockito.when(createPixKeyUseCase.execute(any()))
                .thenReturn(new CreatePixKeyOutput(
                        expectedPixKeyId,
                        aType,
                        aAccountId
                ));

        final var aRequest = MockMvcRequestBuilders.post("/v1/pix-keys")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(ApiTest.admin())
                .content(aRequestBody)
                .header(IdempotencyKey.IDEMPOTENCY_KEY_HEADER, IdentifierUtils.generateNewId())
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expectedPixKeyId))
                .andExpect(jsonPath("$.account_id").value(aAccountId))
                .andExpect(jsonPath("$.type").value(aType));

        Mockito.verify(createPixKeyUseCase, Mockito.times(1)).execute(createPixKeyCommandCaptor.capture());

        final var aCommandCaptured = createPixKeyCommandCaptor.getValue();

        Assertions.assertEquals(aAccountId, aCommandCaptured.accountId());
        Assertions.assertEquals(aType, aCommandCaptured.type());
        Assertions.assertEquals(aValue, aCommandCaptured.value());
    }

    @Test
    void givenAValidValues_whenCallListPixKeys_thenReturnPixKeysPaginated() throws Exception {
        final var aAccountId = IdentifierUtils.generateNewMonotonicULID().toString();

        final var aPixKeyOne = new ListPixKeysOutput(
                IdentifierUtils.generateNewMonotonicULID().toString(),
                aAccountId,
                "12345678900",
                "CPF",
                "ACTIVE",
                InstantUtils.now(),
                InstantUtils.now(),
                null
        );

        final var aPixKeyTwo = new ListPixKeysOutput(
                IdentifierUtils.generateNewMonotonicULID().toString(),
                aAccountId,
                "email@test.com",
                "EMAIL",
                "ACTIVE",
                InstantUtils.now(),
                InstantUtils.now(),
                null
        );

        final var aPage = 0;
        final var aPerPage = 2;
        final var aItemsCount = 2;
        final var aPagesCount = 1;

        final var aMetadata =
                new PaginationMetadata(aPage, aPerPage, aPagesCount, aItemsCount);

        Mockito.when(listPixKeysUseCase.execute(any()))
                .thenReturn(new Pagination<>(aMetadata, List.of(aPixKeyOne, aPixKeyTwo)));

        final var aRequest = MockMvcRequestBuilders.get("/v1/pix-keys")
                .with(ApiTest.admin())
                .queryParam("accountId", aAccountId)
                .queryParam("status", "ACTIVE")
                .queryParam("page", String.valueOf(aPage))
                .queryParam("perPage", String.valueOf(aPerPage))
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.current_page").value(aPage))
                .andExpect(jsonPath("$.metadata.per_page").value(aPerPage))
                .andExpect(jsonPath("$.metadata.total_pages").value(aPagesCount))
                .andExpect(jsonPath("$.metadata.total_items").value(aItemsCount))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].pix_key_id").value(aPixKeyOne.pixKeyId()))
                .andExpect(jsonPath("$.items[0].account_id").value(aPixKeyOne.accountId()))
                .andExpect(jsonPath("$.items[0].value").value(aPixKeyOne.value()))
                .andExpect(jsonPath("$.items[0].type").value(aPixKeyOne.type()))
                .andExpect(jsonPath("$.items[0].status").value(aPixKeyOne.status()));

        Mockito.verify(listPixKeysUseCase, Mockito.times(1)).execute(any());
    }

    @Test
    void givenAValidValue_whenCallGetPixKeyByValue_thenReturnPixKey() throws Exception {
        final var aPixKey = PixKey.newPixKey(new PixKeyValueFactory().create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()));

        final var aValue = aPixKey.getKey().value();

        Mockito.when(getPixKeyByValueUseCase.execute(any()))
                .thenReturn(GetPixKeyByValueOutput.from(aPixKey));

        final var aRequest = MockMvcRequestBuilders.get("/v1/pix-keys/{value}", aValue)
                .with(ApiTest.admin())
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aPixKey.getId().value().toString()))
                .andExpect(jsonPath("$.account_id").value(aPixKey.getAccountId().value().toString()))
                .andExpect(jsonPath("$.value").value(aPixKey.getKey().value()))
                .andExpect(jsonPath("$.type").value(aPixKey.getKey().type().name()))
                .andExpect(jsonPath("$.status").value(aPixKey.getStatus().name()))
                .andExpect(jsonPath("$.created_at").value(aPixKey.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updated_at").value(aPixKey.getUpdatedAt().toString()))
                .andExpect(jsonPath("$.deleted_at").isEmpty());

        Mockito.verify(getPixKeyByValueUseCase, Mockito.times(1)).execute(any());
    }
}
