package com.pnc.masters.pcbinventory.api;

import java.util.List;

public record PcbInventoryLookupsResponse(
        List<PcbInventoryLookupOption> manufacturers,
        List<PcbInventoryLookupOption> vendors
) {
}
