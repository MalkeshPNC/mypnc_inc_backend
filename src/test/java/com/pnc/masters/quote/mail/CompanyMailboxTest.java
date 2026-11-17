package com.pnc.masters.quote.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyMailboxTest {

    @Test
    void failsClearlyWhenTheMailboxIsNotConfigured() {
        QuoteEmailOutbox row = new QuoteEmailOutbox();
        row.setPdfContent("%PDF".getBytes());

        assertThatThrownBy(() -> new CompanyMailbox(new MailSettings()).send(row))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mail is not configured.");
    }
}
