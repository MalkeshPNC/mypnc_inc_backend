package com.pnc.masters.quote.api;

/**
 * The NC master to attach to a saved quote. Only the quote's NC id and number change.
 */
public record QuoteNcChangeRequest(Long ncId) {
}
