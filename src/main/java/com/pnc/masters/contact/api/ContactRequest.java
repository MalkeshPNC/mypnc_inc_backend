package com.pnc.masters.contact.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ContactRequest(
        @NotNull(message = "customerId is required") Long customerId,
        String firstName,
        String lastName,
        String phone,
        @Email(message = "email must be a valid email address") String email,
        String contactPerson
) {
}