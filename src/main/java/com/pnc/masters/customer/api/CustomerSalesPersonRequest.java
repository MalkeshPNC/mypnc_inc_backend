package com.pnc.masters.customer.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustomerSalesPersonRequest(
        @NotNull(message = "salesPersonId is required") Long salesPersonId,
        @Digits(integer = 8, fraction = 2, message = "commission must have up to 8 integer digits and 2 decimals") BigDecimal commission
) {
}