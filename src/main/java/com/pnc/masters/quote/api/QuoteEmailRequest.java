package com.pnc.masters.quote.api;

public record QuoteEmailRequest(String to, String cc, String subject, String body) {
}
