package com.pnc.masters.quote.api;

/**
 * @param attachPdf null and true both attach the stored quote PDF. False sends the message with no file.
 */
public record QuoteEmailRequest(String to, String cc, String subject, String body, Boolean attachPdf) {

    public boolean includePdf() {
        return attachPdf == null || attachPdf;
    }
}
