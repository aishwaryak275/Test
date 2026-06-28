package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** The endpoint needs an authenticated principal but none was present. Maps to 401. */
public class AuthenticationRequiredException extends IamException {
    public AuthenticationRequiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
