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
        assertThat(clean).contains("margin:0");
    }

    @Test
    void dropsScriptsAndUnsafeLinks() {
        String clean = QuoteEmailHtml.clean("<p>Hi</p><script>alert(1)</script><a href=\"javascript:alert(1)\">click</a>");

        assertThat(clean).doesNotContain("script");
        assertThat(clean).doesNotContain("javascript:");
        assertThat(clean).contains("Hi");
    }

    @Test
    void emptyParagraphKeepsDefaultSpacing() {
        String clean = QuoteEmailHtml.clean("<p>Hello</p><p><br></p><p>Next</p>");
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parseBodyFragment(clean);

        assertThat(doc.select("p").get(0).attr("style")).contains("margin:0");
        assertThat(doc.select("p").get(1).hasAttr("style")).isFalse();
        assertThat(doc.select("p").get(2).attr("style")).contains("margin:0");
    }

    @Test
    void treatsEmptyMarkupAsBlank() {
        assertThat(QuoteEmailHtml.isBlank("<p><br></p>")).isTrue();
        assertThat(QuoteEmailHtml.isBlank("<p>Hello</p>")).isFalse();
    }
}
