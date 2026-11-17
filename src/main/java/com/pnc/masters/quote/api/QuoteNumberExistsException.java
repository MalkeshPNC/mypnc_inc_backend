package com.pnc.masters.quote.api;

public class QuoteNumberExistsException extends RuntimeException {

    public QuoteNumberExistsException(String quoteNumber) {
        super("Quote number " + quoteNumber + " already exists");
    }
}
