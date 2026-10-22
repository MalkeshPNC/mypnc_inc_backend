package com.pnc.masters.quote.mail;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class QuoteEmailHtml {

    private static final Safelist ALLOWED = Safelist.none()
            .addTags("p", "br", "div", "span", "b", "strong", "i", "em", "u", "ul", "ol", "li", "h1", "h2", "h3", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    private QuoteEmailHtml() {
    }

    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, ALLOWED);
    }

    public static boolean isBlank(String html) {
        return Jsoup.parse(clean(html)).text().isBlank();
    }
}
