package com.payment.system.infrastructure.rest;

import com.payment.system.domain.pagination.Pagination;
import com.payment.system.infrastructure.pixkeys.req.CreatePixKeyRequest;
import com.payment.system.infrastructure.pixkeys.res.CreatePixKeyResponse;
import com.payment.system.infrastructure.pixkeys.res.GetPixKeyByValueResponse;
import com.payment.system.infrastructure.pixkeys.res.ListPixKeysResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "PixKey API", description = "Endpoints for managing Pix Keys")
@RequestMapping("/v1/pix-keys")
public interface PixKeyAPI {

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a new pix key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "PixKey created successfully"),
            @ApiResponse(responseCode = "400", description = "A validation error was observed"),
            @ApiResponse(responseCode = "422", description = "A business rule was violated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CreatePixKeyResponse> createPixKey(@RequestBody CreatePixKeyRequest request);

    @GetMapping(
            value = "/{value}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get pix key details by Value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PixKey details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "PixKey not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<GetPixKeyByValueResponse> getPixKeyByValue(@PathVariable("value") String value);

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get all pix keys for account authenticated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PixKeys successfully found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    Pagination<ListPixKeysResponse> listPixKeys(
            @RequestParam Map<String, String> filters,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "perPage", required = false, defaultValue = "10") int perPage,
            @RequestParam(name = "sort", required = false, defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate
    );
}
