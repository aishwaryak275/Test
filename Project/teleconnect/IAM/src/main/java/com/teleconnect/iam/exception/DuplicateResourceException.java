package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** An entity with a unique field (e.g. email) already exists. Maps to 409. */
public class DuplicateResourceException extends IamException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
