package com.payment.system.infrastructure.rest.controllers;

import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.rest.TransactionAPI;
import com.payment.system.infrastructure.transactions.req.CreateTransactionRequest;
import com.payment.system.infrastructure.transactions.res.CreateTransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class TransactionRestController implements TransactionAPI {

    private static final Logger log = LoggerFactory.getLogger(TransactionRestController.class);

    private final CreateTransactionUseCase createTransactionUseCase;

    public TransactionRestController(
            final CreateTransactionUseCase createTransactionUseCase
    ) {
        this.createTransactionUseCase = Objects.requireNonNull(createTransactionUseCase);
    }

    @IdempotencyKey
    @Override
    public ResponseEntity<CreateTransactionResponse> createTransaction(final String idempotencyKey, final CreateTransactionRequest request) {
        log.info("Received create transaction request: {}", request);

        final var aCommand = CreateTransactionCommand.with(
                request.fromAccountId(),
                request.pixKey(),
                request.amount(),
                idempotencyKey
        );

        final var aOutput = this.createTransactionUseCase.execute(aCommand);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateTransactionResponse.from(aOutput));
    }
}
