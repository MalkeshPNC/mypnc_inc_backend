package com.pnc.masters.party;

import com.pnc.masters.party.api.PartyInput;
import com.pnc.masters.party.api.PartyValidationException;

import java.util.regex.Pattern;

public final class PartyFields {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private PartyFields() {
    }

    public record Values(
            String code,
            String name,
            String contactName,
            String contactAddress,
            String contactEmail,
            String contactPhone
    ) {
    }

    public static Values parse(PartyInput request) {
        if (request == null) {
            throw new PartyValidationException("Code is required");
        }
        return new Values(
                required(request.code(), 80, "Code"),
                required(request.name(), 200, "Name"),
                optional(request.contactName(), 200, "Contact name"),
                optional(request.contactAddress(), 500, "Contact address"),
                email(request.contactEmail()),
                optional(request.contactPhone(), 40, "Contact phone")
        );
    }

    private static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            throw new PartyValidationException(label + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new PartyValidationException(label + " must be " + max + " characters or less");
        }
        return trimmed;
    }

    private static String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new PartyValidationException(label + " must be " + max + " characters or less");
        }
        return trimmed;
    }

    private static String email(String value) {
        String trimmed = optional(value, 320, "Contact email");
        if (trimmed != null && !EMAIL.matcher(trimmed).matches()) {
            throw new PartyValidationException("Enter a valid contact email");
        }
        return trimmed;
    }
}
