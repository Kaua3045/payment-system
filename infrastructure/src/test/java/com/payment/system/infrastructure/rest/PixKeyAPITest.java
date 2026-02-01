package com.payment.system.infrastructure.rest;

import com.payment.system.ApiTest;
import com.payment.system.ControllerTest;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyCommand;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyOutput;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
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

@ControllerTest(controllers = PixKeyAPI.class)
class PixKeyAPITest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreatePixKeyUseCase createPixKeyUseCase;

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
}
