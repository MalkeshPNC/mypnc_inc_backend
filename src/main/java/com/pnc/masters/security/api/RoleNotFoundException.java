package com.pnc.masters.security.api;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long id) {
        super("Role with id " + id + " was not found");
    }

    public RoleNotFoundException(String code) {
        super("Role " + code + " was not found");
    }
}
