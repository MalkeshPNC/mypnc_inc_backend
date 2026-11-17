package com.pnc.masters.security.api;

public class EmployeeGroupNotFoundException extends RuntimeException {

    public EmployeeGroupNotFoundException(Long id) {
        super("Employee group " + id + " was not found");
    }
}
