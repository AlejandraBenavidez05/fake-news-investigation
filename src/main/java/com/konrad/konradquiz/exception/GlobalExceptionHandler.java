package com.konrad.konradquiz.exception;

import com.konrad.konradquiz.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Translates exceptions into the {@link ErrorResponseDto} body the frontend reads ({@code error.message}).
 * Status codes, {@code error} labels and messages are part of the API contract.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Business Rule Violation", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "Validation Failed", message, request);
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ErrorResponseDto> handleExport(
            ExportException ex, HttpServletRequest request) {
        log.error("CSV export failed on {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Export Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponseDto> handleEncryption(
            EncryptionException ex, HttpServletRequest request) {
        log.error("Encryption error on {}", request.getRequestURI(), ex);
        // Deliberately generic: never expose crypto details to the client
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Encryption Error", "A data security error occurred.", request);
    }

    /** Catch-all; must stay the most generic handler. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again.", request);
    }

    private static ResponseEntity<ErrorResponseDto> error(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponseDto.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
