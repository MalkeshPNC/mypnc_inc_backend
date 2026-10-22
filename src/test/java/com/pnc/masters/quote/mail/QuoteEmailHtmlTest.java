package com.pnc.masters.quote.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteEmailHtmlTest {

    @Test
    void keepsSimpleFormattingAndLinks() {
        String clean = QuoteEmailHtml.clean("<p>Hello <strong>there</strong></p><a href=\"https://example.com\">quote</a>");

        assertThat(clean).contains("Hello");
        assertThat(clean).contains("<strong>there</strong>");
        assertThat(clean).contains("href=\"https://example.com\"");
    }

    @Test
    void dropsScriptsAndUnsafeLinks() {
        String clean = QuoteEmailHtml.clean("<p>Hi</p><script>alert(1)</script><a href=\"javascript:alert(1)\">click</a>");

        assertThat(clean).doesNotContain("script");
        assertThat(clean).doesNotContain("javascript:");
        assertThat(clean).contains("Hi");
    }

    @Test
    void treatsEmptyMarkupAsBlank() {
        assertThat(QuoteEmailHtml.isBlank("<p><br></p>")).isTrue();
        assertThat(QuoteEmailHtml.isBlank("<p>Hello</p>")).isFalse();
    }
}
