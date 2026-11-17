package com.pnc.masters.quote.mail;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        Document doc = Jsoup.parseBodyFragment(Jsoup.clean(html, ALLOWED));
        doc.select("p, ul, ol, h1, h2, h3").forEach(element -> {
            if ("p".equals(element.normalName()) && element.text().isBlank() && element.select("img").isEmpty()) {
                return;
            }
            element.attr("style", "margin:0;mso-margin-top-alt:0;mso-margin-bottom-alt:0;");
        });
        return doc.body().html();
    }

    public static boolean isBlank(String html) {
        return Jsoup.parse(clean(html)).text().isBlank();
    }
}
