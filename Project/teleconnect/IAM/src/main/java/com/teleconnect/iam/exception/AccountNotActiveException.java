package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** The account exists and credentials match, but its status is not Active. Maps to 403. */
public class AccountNotActiveException extends IamException {
    public AccountNotActiveException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
