package com.pnc.masters.customer.api;

import java.math.BigDecimal;

public record CustomerSalesPersonResponse(
        Long salesPersonId,
        String salesPerson,
        String spEmail,
        BigDecimal commission
) {
}