package com.payment.system.infrastructure.rest;

import com.payment.system.ApiTest;
import com.payment.system.ControllerTest;
import com.payment.system.application.usecases.accounts.close.CloseAccountCommand;
import com.payment.system.application.usecases.accounts.close.CloseAccountUseCase;
import com.payment.system.application.usecases.accounts.create.CreateAccountCommand;
import com.payment.system.application.usecases.accounts.create.CreateAccountOutput;
import com.payment.system.application.usecases.accounts.create.CreateAccountUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdOutput;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdUseCase;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
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

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(controllers = AccountAPI.class)
class AccountAPITest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;

    @MockitoBean
    private GetAccountByIdUseCase getAccountByIdUseCase;

    @MockitoBean
    private CloseAccountUseCase closeAccountUseCase;

    @Captor
    private ArgumentCaptor<CreateAccountCommand> createAccountCommandCaptor;

    @Test
    void givenAValidRequest_whenCallsCreateAccount_shouldReturnHttp201() throws Exception {
        final var aUserId = "user-123";

        final var expectedAccountId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var expectedStatus = "ACTIVE";

        final var aRequestBody = """
                {
                    "user_id": "%s"
                }
                """.formatted(aUserId);

        Mockito.when(createAccountUseCase.execute(any()))
                .thenReturn(new CreateAccountOutput(
                        expectedAccountId,
                        aUserId,
                        expectedStatus
                ));

        final var aRequest = MockMvcRequestBuilders.post("/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(ApiTest.admin())
                .content(aRequestBody)
                .header(IdempotencyKey.IDEMPOTENCY_KEY_HEADER, IdentifierUtils.generateNewId())
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expectedAccountId))
                .andExpect(jsonPath("$.user_id").value(aUserId))
                .andExpect(jsonPath("$.status").value(expectedStatus));

        Mockito.verify(createAccountUseCase, Mockito.times(1)).execute(createAccountCommandCaptor.capture());

        final var aCommandCaptured = createAccountCommandCaptor.getValue();

        Assertions.assertEquals(aUserId, aCommandCaptured.userId());
    }

    @Test
    void givenAValidId_whenCallsGetAccountById_shouldReturnHttp200() throws Exception {
        final var aAccount = Account.newAccount("user-123");

        Mockito.when(getAccountByIdUseCase.execute(any()))
                .thenReturn(GetAccountByIdOutput.from(aAccount));

        final var aRequest = MockMvcRequestBuilders.get("/v1/accounts/{accountId}", aAccount.getId().value().toString())
                .with(ApiTest.admin())
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aAccount.getId().value().toString()))
                .andExpect(jsonPath("$.user_id").value(aAccount.getUserId()))
                .andExpect(jsonPath("$.status").value(aAccount.getStatus().name()))
                .andExpect(jsonPath("$.created_at").value(aAccount.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updated_at").value(aAccount.getUpdatedAt().toString()));

        Mockito.verify(getAccountByIdUseCase, Mockito.times(1)).execute(any());
    }

    @Test
    void givenAValidId_whenCallsCloseAccountById_shouldReturnHttp200() throws Exception {
        final var aAccount = Account.newAccount("user-123");

        Mockito.doNothing().when(closeAccountUseCase).execute(CloseAccountCommand.with(aAccount.getId().value().toString()));

        final var aRequest = MockMvcRequestBuilders.delete("/v1/accounts/{accountId}", aAccount.getId().value().toString())
                .with(ApiTest.admin());

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Mockito.verify(closeAccountUseCase, Mockito.times(1)).execute(any());
    }
}
