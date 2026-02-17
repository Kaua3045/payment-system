package com.payment.system.infrastructure.e2e;

import com.payment.system.ApiTest;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.infrastructure.accounts.req.CreateAccountRequest;
import com.payment.system.infrastructure.accounts.res.CreateAccountResponse;
import com.payment.system.infrastructure.accounts.res.GetAccountByIdResponse;
import com.payment.system.infrastructure.configurations.json.Json;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.pixkeys.req.CreatePixKeyRequest;
import com.payment.system.infrastructure.pixkeys.res.CreatePixKeyResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public interface MockDsl {

    MockMvc mvc();

    /* Accounts */

    default CreateAccountResponse givenAnAccount(final String aUserId) throws Exception {
        final var aRequest = new CreateAccountRequest(aUserId);
        return this.givenAsResponse("/v1/accounts", aRequest, CreateAccountResponse.class);
    }

    default ResultActions givenAnAccountResult(final String aUserId) throws Exception {
        final var aRequest = new CreateAccountRequest(aUserId);
        return this.givenResult("/v1/accounts", aRequest);
    }

    default GetAccountByIdResponse retrieveAnAccount(final String anId) throws Exception {
        return this.retrieve("/v1/accounts/", anId, GetAccountByIdResponse.class);
    }

    /* PixKeys */

    default CreatePixKeyResponse givenAnPixKey(final String aType, final String aValue, final String aAccountId) throws Exception {
        final var aRequest = new CreatePixKeyRequest(aType, aValue, aAccountId);
        return this.givenAsResponse("/v1/pix-keys", aRequest, CreatePixKeyResponse.class);
    }

    default ResultActions givenAnPixKeyResult(final String aType, final String aValue, final String aAccountId) throws Exception {
        final var aRequest = new CreatePixKeyRequest(aType, aValue, aAccountId);
        return this.givenResult("/v1/pix-keys", aRequest);
    }

    default ResultActions listPixKeys(final int aPage, final int aPerPage, final Map<String, String> aFilters) throws Exception {
        return listPixKeys(aPage, aPerPage, "", "", "", "", "", aFilters);
    }

    default ResultActions listPixKeys(final int aPage, final int aPerPage, final String aSearch, final Map<String, String> aFilters) throws Exception {
        return listPixKeys(aPage, aPerPage, aSearch, "", "", "", "", aFilters);
    }

    default ResultActions listPixKeys(final int aPage, final int aPerPage, final String aSearch, final String aSort, final String aDirection, final String aStartDate, final String aEndDate, final Map<String, String> aFilters) throws Exception {
        return this.list("/v1/pix-keys", aPage, aPerPage, aSearch, aSort, aDirection, aStartDate, aEndDate, aFilters);
    }

    private <T> T givenAsResponse(final String aUrl, final Object aBody, final Class<T> aClazz) throws Exception {
        final var aRequest = post(aUrl)
                .with(ApiTest.admin())
                .header(IdempotencyKey.IDEMPOTENCY_KEY_HEADER, IdentifierUtils.generateNewId())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.writeValueAsString(aBody));

        final var aJson = this.mvc().perform(aRequest)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse().getContentAsString();

        return Json.readValue(aJson, aClazz);
    }

    private ResultActions givenResult(final String aUrl, final Object aBody) throws Exception {
        final var aRequest = post(aUrl)
                .with(ApiTest.admin())
                .header(IdempotencyKey.IDEMPOTENCY_KEY_HEADER, IdentifierUtils.generateNewId())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.writeValueAsString(aBody));

        return this.mvc().perform(aRequest);
    }

    private <T> T retrieve(final String aUrl, final String anId, final Class<T> aClazz) throws Exception {
        final var aRequest = get(aUrl + anId)
                .with(ApiTest.admin())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        final var aJson = this.mvc().perform(aRequest)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        return Json.readValue(aJson, aClazz);
    }

    private ResultActions list(
            final String aUrl,
            final int aPage,
            final int aPerPage,
            final String aSearch,
            final String aSort,
            final String aDirection,
            final String aStartDate,
            final String aEndDate,
            final Map<String, String> filters
    ) throws Exception {
        MockHttpServletRequestBuilder aRequest = get(aUrl)
                .with(ApiTest.admin())
                .queryParam("page", String.valueOf(aPage))
                .queryParam("perPage", String.valueOf(aPerPage))
                .queryParam("search", aSearch)
                .queryParam("sort", aSort)
                .queryParam("direction", aDirection)
                .queryParam("startDate", aStartDate)
                .queryParam("endDate", aEndDate)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        if (filters != null) {
            filters.forEach((key, value) -> {
                if (value != null) aRequest.queryParam(key, value);
            });
        }

        return this.mvc().perform(aRequest);
    }
}
