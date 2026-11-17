package com.pnc.masters.quote.api;

public class QuoteEmailLogNotFoundException extends RuntimeException {

    public QuoteEmailLogNotFoundException(Long id) {
        super("Email log " + id + " was not found");
    }
}
