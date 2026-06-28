package com.teleconnect.iam.exception;

import org.springframework.http.HttpStatus;

/** The request is well-formed but carries an invalid value (e.g. bad status code). Maps to 400. */
public class InvalidRequestException extends IamException {
    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
