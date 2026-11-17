package com.pnc.masters.quote.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuotePdfNamesTest {

    @Test
    void keepsSafeQuoteNumbers() {
        assertThat(QuotePdfNames.fileName("Q-100", 4L)).isEqualTo("Quote-Q-100.pdf");
        assertThat(QuotePdfNames.storageKey("Q-100", 4L)).isEqualTo("quote-pdf/Q-100/Quote-Q-100.pdf");
    }

    @Test
    void stripsCharactersThatCannotSitInAPath() {
        assertThat(QuotePdfNames.storageKey("Q/100:A", 4L)).isEqualTo("quote-pdf/Q100A/Quote-Q100A.pdf");
    }

    @Test
    void usesTheQuoteIdWhenTheNumberHasNoSafeCharacters() {
        assertThat(QuotePdfNames.fileName("///", 12L)).isEqualTo("Quote-Q12.pdf");
        assertThat(QuotePdfNames.storageKey(null, 12L)).isEqualTo("quote-pdf/Q12/Quote-Q12.pdf");
    }
}
