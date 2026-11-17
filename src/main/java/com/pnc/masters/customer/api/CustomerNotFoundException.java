package com.pnc.masters.customer.api;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Customer with id " + id + " was not found");
    }

    public CustomerNotFoundException(String name) {
        super("Customer " + name + " was not found");
    }
}
