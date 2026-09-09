package com.pnc.masters.quote.api;

import java.math.BigDecimal;

public record QuoteQuantityResponse(
        Long qtyId,
        Long qid,
        String lt,
        Integer ltPcb,
        Integer ltPcba,
        Integer qty,
        BigDecimal pcbCost,
        BigDecimal partsCost,
        BigDecimal laborCost,
        BigDecimal laborDiscount,
        BigDecimal partMarkup,
        BigDecimal testing,
        BigDecimal confCoat,
        String serviceName,
        BigDecimal serviceCharge,
        BigDecimal subTotal,
        BigDecimal totalSalesPct,
        BigDecimal pcbNre,
        BigDecimal assyNre,
        BigDecimal stencil,
        BigDecimal otherNre,
        BigDecimal total,
        String comments,
        boolean received
) {
}
