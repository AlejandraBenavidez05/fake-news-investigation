package com.konrad.konradquiz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}