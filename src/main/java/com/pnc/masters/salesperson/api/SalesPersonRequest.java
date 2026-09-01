package com.pnc.masters.salesperson.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SalesPersonRequest(
        @NotBlank(message = "salesPerson is required") String salesPerson,
        @Email(message = "spEmail must be a valid email address") String spEmail
) {
}