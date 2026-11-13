package com.pnc.masters.pcbinventory.api;

import java.util.List;

public record PcbInventoryDetailResponse(
        PcbInventoryItemResponse item,
        List<PcbInventoryTxnResponse> transactions
) {
}
