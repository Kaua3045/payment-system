package com.payment.system.infrastructure.rest.controllers;

import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyCommand;
import com.payment.system.application.usecases.pixkeys.create.CreatePixKeyUseCase;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.pixkeys.req.CreatePixKeyRequest;
import com.payment.system.infrastructure.pixkeys.res.CreatePixKeyResponse;
import com.payment.system.infrastructure.rest.PixKeyAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class PixKeyRestController implements PixKeyAPI {

    private static final Logger log = LoggerFactory.getLogger(PixKeyRestController.class);

    private final CreatePixKeyUseCase createPixKeyUseCase;

    public PixKeyRestController(
            final CreatePixKeyUseCase createPixKeyUseCase
    ) {
        this.createPixKeyUseCase = Objects.requireNonNull(createPixKeyUseCase);
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreatePixKeyResponse.from(aOutput));
    }
}
