package com.pnc.masters.pcbinventory.api;

import java.math.BigDecimal;

public record PcbInventoryItemResponse(
        Long itemId,
        String description,
        String itemType,
        String location,
        Long manufacturerId,
        String manufacturerName,
        String sheetSize,
        int available,
        int allocated,
        int onOrder,
        String leadTime,
        int reorderLevel,
        int reorderQty,
        String alertOnItem,
        BigDecimal stdPrice,
        String comments
) {
}
