package com.payment.system.infrastructure.rest;

import com.payment.system.infrastructure.accounts.req.CreateAccountRequest;
import com.payment.system.infrastructure.accounts.res.CreateAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Account API", description = "Endpoints for managing accounts")
@RequestMapping("/v1/accounts")
public interface AccountAPI {

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a new account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "A validation error was observed"),
            @ApiResponse(responseCode = "422", description = "A business rule was violated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CreateAccountResponse> createAccount(@RequestBody CreateAccountRequest request);
}
