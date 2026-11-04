package com.pnc.masters.quote.api;

import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * A quantity line as submitted by the form. A null or zero {@code qtyId} means
 * the row is new and the server assigns the real id. Sub total, Total (S%) and
 * Total are absent because the server derives them from the costs below.
 */
public record QuoteQuantityRequest(
        Long qtyId,
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
        BigDecimal pcbNre,
        BigDecimal assyNre,
        BigDecimal stencil,
        BigDecimal otherNre,
        String comments,
        Boolean received,
        @Digits(integer = 4, fraction = 2, message = "commissionPercentage must have up to 4 integer digits and 2 decimals")
        BigDecimal commissionPercentage
) {
}
