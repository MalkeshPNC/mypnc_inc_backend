package com.pnc.masters.security.api;

public class UserValidationException extends RuntimeException {

    public UserValidationException(String message) {
        super(message);
    }
}
