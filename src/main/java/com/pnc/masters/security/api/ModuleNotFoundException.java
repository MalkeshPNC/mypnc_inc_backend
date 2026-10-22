package com.pnc.masters.security.api;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(Long id) {
        super("Module " + id + " was not found");
    }
}
