package com.payment.system.infrastructure.rest.controllers;

import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositCommand;
import com.payment.system.application.usecases.transactions.deposit.CreateDepositUseCase;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdCommand;
import com.payment.system.application.usecases.transactions.retrieve.id.GetTransactionByIdUseCase;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.rest.TransactionAPI;
import com.payment.system.infrastructure.transactions.req.CreateDepositRequest;
import com.payment.system.infrastructure.transactions.req.CreateTransactionRequest;
import com.payment.system.infrastructure.transactions.res.CreateDepositResponse;
import com.payment.system.infrastructure.transactions.res.CreateTransactionResponse;
import com.payment.system.infrastructure.transactions.res.GetTransactionByIdResponse;
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
    private final GetTransactionByIdUseCase getTransactionByIdUseCase;
    private final CreateDepositUseCase createDepositUseCase;

    public TransactionRestController(
            final CreateTransactionUseCase createTransactionUseCase,
            final GetTransactionByIdUseCase getTransactionByIdUseCase,
            final CreateDepositUseCase createDepositUseCase
    ) {
        this.createTransactionUseCase = Objects.requireNonNull(createTransactionUseCase);
        this.getTransactionByIdUseCase = Objects.requireNonNull(getTransactionByIdUseCase);
        this.createDepositUseCase = Objects.requireNonNull(createDepositUseCase);
    }

    @IdempotencyKey
    @Override
    public ResponseEntity<CreateTransactionResponse> createTransaction(final String idempotencyKey, final CreateTransactionRequest request) {
        log.info("Received create transaction request: {}", request);

        final var aCommand = CreateTransactionCommand.with(
                request.fromAccountId(),
                request.pixKey(),
                request.pixKeyType(),
                request.amount(),
                idempotencyKey
        );

        final var aOutput = this.createTransactionUseCase.execute(aCommand);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateTransactionResponse.from(aOutput));
    }

    @IdempotencyKey
    @Override
    public ResponseEntity<CreateDepositResponse> createDeposit(final String idempotencyKey, final CreateDepositRequest request) {
        log.info("Received create deposit request: {}", request);

        final var aCommand = CreateDepositCommand.with(
                request.pixKey(),
                request.pixKeyType(),
                request.source(),
                request.amount(),
                idempotencyKey
        );

        final var aOutput = this.createDepositUseCase.execute(aCommand);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateDepositResponse.from(aOutput));
    }

    @Override
    public ResponseEntity<GetTransactionByIdResponse> getTransactionByIdAndAuthenticatedUser(final String accountId, final String transactionId) {
        final var aCommand = GetTransactionByIdCommand.with(transactionId, accountId);

        final var aOutput = this.getTransactionByIdUseCase.execute(aCommand);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GetTransactionByIdResponse.from(aOutput));
    }
}
