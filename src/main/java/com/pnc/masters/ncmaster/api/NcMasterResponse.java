package com.pnc.masters.ncmaster.api;

import java.time.LocalDateTime;

public record NcMasterResponse(
        Long ncId,
        String ncNumber,
        String pcbPartNumber,
        String pcbRev,
        String pcbaPartNumber,
        String pcbaRev,
        String notes,
        String ncAlert,
        String pcbaNotes,
        String createdBy,
        Long createdByUserId,
        LocalDateTime createdAt
) {
}
