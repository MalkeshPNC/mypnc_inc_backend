package com.pnc.masters.quote.application;

import com.pnc.masters.quote.Quote;

/** Places the preview PDF in the quote's attachment folder. */
public interface QuoteGeneratedPdfStore {

    void storeGeneratedPdf(Quote quote, byte[] pdf);
}
