package com.exercise.atm.api.error;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String message;
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.message = message;
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
