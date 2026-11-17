package com.pnc.masters.quote.api;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(Long id) {
        super("Quote " + id + " was not found");
    }
}
