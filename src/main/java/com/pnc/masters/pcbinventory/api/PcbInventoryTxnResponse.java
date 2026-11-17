package com.pnc.masters.pcbinventory.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PcbInventoryTxnResponse(
        Long txnId,
        String transType,
        String vendorName,
        String sourceName,
        String sourceNumber,
        int inQty,
        int outQty,
        BigDecimal price,
        String actionBy,
        LocalDateTime actionDate
) {
}
