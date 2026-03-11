package com.konrad.konradquiz.exception;

import com.konrad.konradquiz.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("Business Rule Violation")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ErrorResponseDto> handleExport(
            ExportException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Export Failed")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponseDto> handleEncryption(
            EncryptionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Encryption Error")
                        .message("A data security error occurred.")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)  // ← LAST always
    public ResponseEntity<ErrorResponseDto> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message("An unexpected error occurred. Please try again.")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}