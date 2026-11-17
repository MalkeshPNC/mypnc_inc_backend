package com.pnc.masters.security.api;

public class RoleConflictException extends RuntimeException {

    public RoleConflictException(String message) {
        super(message);
    }
}
