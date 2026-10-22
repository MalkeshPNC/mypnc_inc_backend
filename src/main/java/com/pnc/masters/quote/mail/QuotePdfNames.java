package com.pnc.masters.quote.mail;

public final class QuotePdfNames {

    private QuotePdfNames() {
    }

    public static String fileName(String quoteNumber, Long qid) {
        return "Quote-" + token(quoteNumber, qid) + ".pdf";
    }

    public static String storageKey(String quoteNumber, Long qid) {
        String safe = token(quoteNumber, qid);
        return "quote-pdf/" + safe + "/" + fileName(quoteNumber, qid);
    }

    static String token(String quoteNumber, Long qid) {
        String raw = quoteNumber == null ? "" : quoteNumber;
        String safe = raw.replaceAll("[^A-Za-z0-9._-]", "");
        if (!safe.isBlank()) {
            return safe;
        }
        return "Q" + (qid == null ? "0" : qid);
    }
}
