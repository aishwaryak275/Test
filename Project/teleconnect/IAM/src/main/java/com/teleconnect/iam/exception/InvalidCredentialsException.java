package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** Supplied credentials (login password or current password) are wrong. Maps to 401. */
public class InvalidCredentialsException extends IamException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
