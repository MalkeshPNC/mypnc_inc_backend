package com.pnc.masters.salesperson.api;

public class SalesPersonInUseException extends RuntimeException {

    public SalesPersonInUseException(Long id) {
        super("Salesperson with id " + id + " is assigned to one or more customers and cannot be deleted");
    }
}
