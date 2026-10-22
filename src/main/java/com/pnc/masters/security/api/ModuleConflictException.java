package com.pnc.masters.security.api;

public class ModuleConflictException extends RuntimeException {
    public ModuleConflictException(String message) {
        super(message);
    }
}
