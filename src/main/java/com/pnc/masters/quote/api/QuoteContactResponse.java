package com.pnc.masters.quote.api;

public record QuoteContactResponse(Long contId, String name, String email, boolean primary) {
}
