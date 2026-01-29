package com.kaua.template.infrastructure.configurations;

import com.kaua.template.application.exceptions.UseCaseInputCannotBeNullException;
import com.kaua.template.domain.exceptions.DomainException;
import com.kaua.template.domain.exceptions.InternalErrorException;
import com.kaua.template.domain.exceptions.NotFoundException;
import com.kaua.template.domain.exceptions.ValidationException;
import com.kaua.template.domain.utils.InstantUtils;
import com.kaua.template.infrastructure.exceptions.IdempotencyKeyUnsupportedMethodException;
import com.kaua.template.infrastructure.utils.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(final ValidationException ex) {
        log.debug("Handling validation exception: {}, errors {}", ex.getMessage(), ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.from(ex, InstantUtils.now()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(final DomainException ex) {
        log.debug("Handling domain exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.from(ex, InstantUtils.now()));
    }

    @ExceptionHandler(UseCaseInputCannotBeNullException.class)
    public ResponseEntity<ApiError> handleUseCaseInputCannotBeNullException(final UseCaseInputCannotBeNullException ex) {
        log.debug("Handling use case input cannot be null exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.from(ex.getMessage(), InstantUtils.now()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(final NotFoundException ex) {
        log.debug("Handling not found exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.from(ex.getMessage(), InstantUtils.now()));
    }

    @ExceptionHandler(IdempotencyKeyUnsupportedMethodException.class)
    public ResponseEntity<ApiError> handleIdempotencyKeyUnsupportedMethodException(final IdempotencyKeyUnsupportedMethodException ex) {
        log.debug("Handling idempotency key unsupported method exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.from(ex.getMessage(), InstantUtils.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(final Exception ex) {
        log.error("Handling unexpected exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.from("An unexpected error occurred", InstantUtils.now()));
    }

    @ExceptionHandler(InternalErrorException.class)
    public ResponseEntity<ApiError> handleInternalErrorException(final InternalErrorException ex) {
        log.error("Handling internal error exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.from(ex.getMessage(), InstantUtils.now()));
    }
}
