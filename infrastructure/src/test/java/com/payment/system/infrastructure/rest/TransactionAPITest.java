package com.payment.system.infrastructure.rest;

import com.payment.system.ApiTest;
import com.payment.system.ControllerTest;
import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionOutput;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.domain.transactions.TransactionStatus;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(controllers = TransactionAPI.class)
class TransactionAPITest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateTransactionUseCase createTransactionUseCase;

    @Captor
    private ArgumentCaptor<CreateTransactionCommand> createTransactionCommandCaptor;

    @Test
    void givenAValidRequest_whenCallsCreateTransaction_shouldReturnHttp201() throws Exception {
        final var aFromAccountId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var aPixKey = "61268368712361";
        final var aAmount = new BigDecimal("10.50");

        final var aIdempotencyKey = IdentifierUtils.generateNewId();

        final var expectedTransactionId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var expectedStatus = TransactionStatus.COMPLETED.name();

        final var aRequestBody = """
                {
                    "from_account_id": "%s",
                    "pix_key": "%s",
                    "amount": "%s"
                }
                """.formatted(aFromAccountId, aPixKey, aAmount);

        Mockito.when(createTransactionUseCase.execute(any()))
                .thenReturn(new CreateTransactionOutput(
                        expectedTransactionId,
                        expectedStatus
                ));

        final var aRequest = MockMvcRequestBuilders.post("/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(ApiTest.admin())
                .content(aRequestBody)
                .header(IdempotencyKey.IDEMPOTENCY_KEY_HEADER, aIdempotencyKey)
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction_id").value(expectedTransactionId))
                .andExpect(jsonPath("$.status").value(expectedStatus));

        Mockito.verify(createTransactionUseCase, Mockito.times(1)).execute(createTransactionCommandCaptor.capture());

        final var aCommandCaptured = createTransactionCommandCaptor.getValue();

        Assertions.assertEquals(aFromAccountId, aCommandCaptured.fromAccountId());
        Assertions.assertEquals(aPixKey, aCommandCaptured.pixKey());
        Assertions.assertEquals(aAmount, aCommandCaptured.amount());
        Assertions.assertEquals(aIdempotencyKey, aCommandCaptured.idempotencyKey());
    }
}
