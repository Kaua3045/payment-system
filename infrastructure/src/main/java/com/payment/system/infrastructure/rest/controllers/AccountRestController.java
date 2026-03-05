package com.payment.system.infrastructure.rest.controllers;

import com.payment.system.application.usecases.accounts.create.CreateAccountCommand;
import com.payment.system.application.usecases.accounts.create.CreateAccountUseCase;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdCommand;
import com.payment.system.application.usecases.accounts.retrieve.id.GetAccountByIdUseCase;
import com.payment.system.infrastructure.accounts.req.CreateAccountRequest;
import com.payment.system.infrastructure.accounts.res.CreateAccountResponse;
import com.payment.system.infrastructure.accounts.res.GetAccountByIdResponse;
import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.rest.AccountAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class AccountRestController implements AccountAPI {

    private static final Logger log = LoggerFactory.getLogger(AccountRestController.class);

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountByIdUseCase getAccountByIdUseCase;

    public AccountRestController(
            final CreateAccountUseCase createAccountUseCase,
            final GetAccountByIdUseCase getAccountByIdUseCase
    ) {
        this.createAccountUseCase = Objects.requireNonNull(createAccountUseCase);
        this.getAccountByIdUseCase = Objects.requireNonNull(getAccountByIdUseCase);
    }

    @IdempotencyKey
    @Override
    public ResponseEntity<CreateAccountResponse> createAccount(final CreateAccountRequest request) {
        log.info("Received create account request: {}", request);

        final var aCommand = CreateAccountCommand.with(request.userId());

        final var aOutput = this.createAccountUseCase.execute(aCommand);

        log.debug("Create account request processed successfully: {}", aOutput);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateAccountResponse.from(aOutput));
    }

    @Override
    public ResponseEntity<GetAccountByIdResponse> getAccountById(final String accountId) {
        final var aOutput = this.getAccountByIdUseCase.execute(GetAccountByIdCommand.with(accountId));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GetAccountByIdResponse.from(aOutput));
    }
}
