package com.pnc.masters.salesperson.api;

public class SalesPersonNotFoundException extends RuntimeException {

    public SalesPersonNotFoundException(Long id) {
        super("Salesperson with id " + id + " was not found");
    }
}