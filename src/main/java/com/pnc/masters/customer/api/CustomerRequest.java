package com.pnc.masters.customer.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDateTime;

public record CustomerRequest(
        @NotBlank(message = "customer is required") String customer,
        String companyLogo,
        @Valid List<CustomerSalesPersonRequest> salesPersons,
        LocalDateTime custEntryDt,
        String referredBy,
        String remarks,
        String address,
        String city,
        String state,
        String zip,
        String billtoAddress,
        String shiptoAddress,
        Boolean automailOn,
        @Digits(integer = 8, fraction = 2, message = "salesPersonDefaultCommission must have up to 8 integer digits and 2 decimals")
        BigDecimal salesPersonDefaultCommission
) {
}
