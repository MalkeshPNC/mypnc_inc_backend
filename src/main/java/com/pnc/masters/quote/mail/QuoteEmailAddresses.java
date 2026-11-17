package com.pnc.masters.quote.mail;

import com.pnc.masters.quote.api.QuoteValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class QuoteEmailAddresses {

    static final int MAX_RECIPIENTS = 10;
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private QuoteEmailAddresses() {
    }

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> emails = new ArrayList<>();
        for (String part : raw.split("[,;]")) {
            String email = part.trim();
            if (email.isEmpty()) {
                continue;
            }
            if (!EMAIL.matcher(email).matches()) {
                throw new QuoteValidationException("Enter a valid email address.");
            }
            emails.add(email.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(emails);
    }

    public static void requireLimits(List<String> to, List<String> cc) {
        if (to.isEmpty()) {
            throw new QuoteValidationException("Enter at least one To address.");
        }
        if (to.size() + cc.size() > MAX_RECIPIENTS) {
            throw new QuoteValidationException("A quote email can include at most 10 addresses.");
        }
    }
}
