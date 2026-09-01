package com.pnc.masters.customer.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        Long custId,
        String customer,
        String companyLogo,
        List<CustomerSalesPersonResponse> salesPersons,
        LocalDateTime custEntryDt,
        String referredBy,
        String remarks,
        String address,
        String city,
        String state,
        String zip,
        String billtoAddress,
        String shiptoAddress,
        boolean automailOn,
        BigDecimal salesPersonDefaultCommission
) {
}
