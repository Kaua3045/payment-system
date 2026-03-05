package com.payment.system.infrastructure.rest.controllers;

import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyCommand;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
import com.payment.system.application.usecases.pixkeys.retrieve.list.ListPixKeysUseCase;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;
import com.payment.system.domain.utils.Period;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.pixkeys.req.CreatePixKeyRequest;
import com.payment.system.infrastructure.pixkeys.res.CreatePixKeyResponse;
import com.payment.system.infrastructure.pixkeys.res.ListPixKeysResponse;
import com.payment.system.infrastructure.rest.PixKeyAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@RestController
public class PixKeyRestController implements PixKeyAPI {

    private static final Logger log = LoggerFactory.getLogger(PixKeyRestController.class);

    private final CreatePixKeyUseCase createPixKeyUseCase;
    private final ListPixKeysUseCase listPixKeysUseCase;

    public PixKeyRestController(
            final CreatePixKeyUseCase createPixKeyUseCase,
            final ListPixKeysUseCase listPixKeysUseCase
    ) {
        this.createPixKeyUseCase = Objects.requireNonNull(createPixKeyUseCase);
        this.listPixKeysUseCase = Objects.requireNonNull(listPixKeysUseCase);
    }

    @IdempotencyKey
    @Override
    public ResponseEntity<CreatePixKeyResponse> createPixKey(final CreatePixKeyRequest request) {
        log.info("Received create pix key request: {}", request);

        final var aCommand = CreatePixKeyCommand.with(
                request.type(),
                request.value(),
                request.accountId()
        );

        final var aOutput = this.createPixKeyUseCase.execute(aCommand);

        log.debug("Pix key created successfully: {}", aOutput);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreatePixKeyResponse.from(aOutput));
    }

    @Override
    public Pagination<ListPixKeysResponse> listPixKeys(
            final Map<String, String> filters,
            final String search,
            final int page,
            final int perPage,
            final String sort,
            final String direction,
            final String startDate,
            final String endDate
    ) {
        final var aQuery = SearchQuery.newSearchQuery(
                page,
                perPage,
                search,
                sort,
                direction,
                new Period(Period.startValidate(
                        startDate,
                        30,
                        ChronoUnit.DAYS
                ), Period.endValidate(
                        endDate,
                        30,
                        ChronoUnit.DAYS
                )),
                filters
        );

        final var aOutput = this.listPixKeysUseCase.execute(aQuery);

        return aOutput.map(ListPixKeysResponse::from);
    }
}
