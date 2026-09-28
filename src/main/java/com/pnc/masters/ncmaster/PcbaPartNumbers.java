package com.pnc.masters.ncmaster;

import java.util.regex.Pattern;

/**
 * PCBA part numbers are letters and digits, plus a short set of separators.
 */
public final class PcbaPartNumbers {

    public static final String MESSAGE =
            "PCBA Part Number can only include letters, numbers, and - _ . / + #.";

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/+#\\-]*$");

    private PcbaPartNumbers() {
    }

    public static boolean isValid(String value) {
        return value != null && PATTERN.matcher(value).matches();
    }
}
