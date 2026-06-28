package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** A requested entity (user, role, ...) does not exist. Maps to 404. */
public class ResourceNotFoundException extends IamException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
