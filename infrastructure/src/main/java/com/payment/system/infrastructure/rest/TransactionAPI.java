package com.payment.system.infrastructure.rest;

import com.payment.system.infrastructure.idempotency.IdempotencyKey;
import com.payment.system.infrastructure.transactions.req.CreateDepositRequest;
import com.payment.system.infrastructure.transactions.req.CreateTransactionRequest;
import com.payment.system.infrastructure.transactions.res.CreateDepositResponse;
import com.payment.system.infrastructure.transactions.res.CreateTransactionResponse;
import com.payment.system.infrastructure.transactions.res.GetTransactionByIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transaction API", description = "Endpoints for managing Transaction")
@RequestMapping("/v1/transactions")
public interface TransactionAPI {

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a new transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "A validation error was observed"),
            @ApiResponse(responseCode = "422", description = "A business rule was violated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CreateTransactionResponse> createTransaction(@RequestHeader(IdempotencyKey.IDEMPOTENCY_KEY_HEADER) String idempotencyKey, @RequestBody CreateTransactionRequest request);

    @PostMapping(
            value = "/deposit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a new transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "A validation error was observed"),
            @ApiResponse(responseCode = "422", description = "A business rule was violated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CreateDepositResponse> createDeposit(@RequestHeader(IdempotencyKey.IDEMPOTENCY_KEY_HEADER) String idempotencyKey, @RequestBody CreateDepositRequest request);

    @GetMapping(
            value = "/{accountId}/{transactionId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get transactions details by ID and authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<GetTransactionByIdResponse> getTransactionByIdAndAuthenticatedUser(@PathVariable("accountId") String accountId, @PathVariable("transactionId") String transactionId);
}
