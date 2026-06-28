package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all IAM business exceptions. Each subclass carries the HTTP
 * status it should map to, so {@link GlobalExceptionHandler} can translate any
 * IAM exception to a response without a per-type handler.
 */
public abstract class IamException extends RuntimeException {

    private final HttpStatus status;

    protected IamException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
