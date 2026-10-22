package com.pnc.masters.quote.mail;

public interface QuoteMailbox {

    void send(QuoteEmailOutbox message);
}
