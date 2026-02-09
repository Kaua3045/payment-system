package com.payment.system.infrastructure.rest;

import com.payment.system.ApiTest;
import com.payment.system.ControllerTest;
import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionOutput;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositCommand;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositOutput;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdOutput;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdUseCase;
import com.payment.system.domain.accounts.AccountId;
import com.payment.system.domain.pixkeys.PixKeyId;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionStatus;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
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

    @MockitoBean
    private GetTransactionByIdUseCase getTransactionByIdUseCase;

    @MockitoBean
    private CreateDepositUseCase createDepositUseCase;

    @Captor
    private ArgumentCaptor<CreateTransactionCommand> createTransactionCommandCaptor;

    @Captor
    private ArgumentCaptor<CreateDepositCommand> createDepositCommandCaptor;

    @Test
    void givenAValidRequest_whenCallsCreateTransaction_shouldReturnHttp201() throws Exception {
        final var aFromAccountId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var aPixKey = "61268368712361";
        final var aPixKeyType = "cpf";
        final var aAmount = new BigDecimal("10.50");

        final var aIdempotencyKey = IdentifierUtils.generateNewId();

        final var expectedTransactionId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var expectedStatus = TransactionStatus.COMPLETED.name();
        final var expectedType = TransactionType.TRANSFER.name();

        final var aRequestBody = """
                {
                    "from_account_id": "%s",
                    "pix_key": "%s",
                    "pix_key_type": "%s",
                    "amount": "%s"
                }
                """.formatted(aFromAccountId, aPixKey, aPixKeyType, aAmount);

        Mockito.when(createTransactionUseCase.execute(any()))
                .thenReturn(new CreateTransactionOutput(
                        expectedTransactionId,
                        expectedStatus,
                        expectedType
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
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.type").value(expectedType));

        Mockito.verify(createTransactionUseCase, Mockito.times(1)).execute(createTransactionCommandCaptor.capture());

        final var aCommandCaptured = createTransactionCommandCaptor.getValue();

        Assertions.assertEquals(aFromAccountId, aCommandCaptured.fromAccountId());
        Assertions.assertEquals(aPixKey, aCommandCaptured.pixKey());
        Assertions.assertEquals(aAmount, aCommandCaptured.amount());
        Assertions.assertEquals(aIdempotencyKey, aCommandCaptured.idempotencyKey());
    }

    @Test
    void givenAValidIds_whenCallsGetTransactionByIdAndAuthenticatedUser_shouldReturnHttp200() throws Exception {
        final var aTransaction = Transaction.newTransaction(
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new AccountId(IdentifierUtils.generateNewMonotonicULID()),
                new PixKeyId(IdentifierUtils.generateNewMonotonicULID()),
                new Money(BigDecimal.TEN),
                TransactionType.TRANSFER,
                DepositSource.EXTERNAL,
                "1238712712678368126834"
        );

        Mockito.when(getTransactionByIdUseCase.execute(any()))
                .thenReturn(GetTransactionByIdOutput.from(aTransaction));

        final var aRequest = MockMvcRequestBuilders.get("/v1/transactions/{accountId}/{transactionId}",
                        aTransaction.getFromAccountId().value().toString(), aTransaction.getId().value().toString())
                .with(ApiTest.admin())
                .accept(MediaType.APPLICATION_JSON_VALUE);

        final var aResponse = this.mvc.perform(aRequest);

        aResponse
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction_id").value(aTransaction.getId().value().toString()))
                .andExpect(jsonPath("$.from_account_id").value(aTransaction.getFromAccountId().value().toString()))
                .andExpect(jsonPath("$.to_account_id").value(aTransaction.getToAccountId().value().toString()))
                .andExpect(jsonPath("$.pix_key_id").value(aTransaction.getPixKeyId().value().toString()))
                .andExpect(jsonPath("$.status").value(aTransaction.getStatus().name()))
                .andExpect(jsonPath("$.type").value(aTransaction.getType().name()))
                .andExpect(jsonPath("$.idempotency_key").value(aTransaction.getIdempotencyKey()))
                .andExpect(jsonPath("$.created_at").value(aTransaction.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updated_at").value(aTransaction.getUpdatedAt().toString()));

        Mockito.verify(getTransactionByIdUseCase, Mockito.times(1)).execute(any());
    }

    @Test
    void givenAValidRequest_whenCallsCreateDeposit_shouldReturnHttp201() throws Exception {
        final var aPixKey = "61268368712361";
        final var aPixKeyType = "cpf";
        final var aSource = "atm";
        final var aAmount = new BigDecimal("10.50");

        final var aIdempotencyKey = IdentifierUtils.generateNewId();

        final var expectedTransactionId = IdentifierUtils.generateNewMonotonicULID().toString();
        final var expectedStatus = TransactionStatus.COMPLETED.name();
        final var expectedType = TransactionType.TRANSFER.name();

        final var aRequestBody = """
                {
                    "pix_key": "%s",
                    "pix_key_type": "%s",
                    "source": "%s",
                    "amount": "%s"
                }
                """.formatted(aPixKey, aPixKeyType, aSource, aAmount);

        Mockito.when(createDepositUseCase.execute(any()))
                .thenReturn(new CreateDepositOutput(
                        expectedTransactionId,
                        expectedStatus,
                        expectedType
                ));

        final var aRequest = MockMvcRequestBuilders.post("/v1/transactions/deposit")
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
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.type").value(expectedType));

        Mockito.verify(createDepositUseCase, Mockito.times(1)).execute(createDepositCommandCaptor.capture());

        final var aCommandCaptured = createDepositCommandCaptor.getValue();

        Assertions.assertEquals(aPixKeyType, aCommandCaptured.pixKeyType());
        Assertions.assertEquals(aPixKey, aCommandCaptured.pixKey());
        Assertions.assertEquals(aAmount, aCommandCaptured.amount());
        Assertions.assertEquals(aSource, aCommandCaptured.source());
        Assertions.assertEquals(aIdempotencyKey, aCommandCaptured.idempotencyKey());
    }
}
