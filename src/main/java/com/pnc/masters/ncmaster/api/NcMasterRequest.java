package com.pnc.masters.ncmaster.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NcMasterRequest(
        @NotBlank @Size(max = 80) String ncNumber,
        @Size(max = 120) String pcbPartNumber,
        @Size(max = 40) String pcbRev,
        @Size(max = 120) String pcbaPartNumber,
        @Size(max = 40) String pcbaRev,
        String pcbaAlert,
        String notes,
        String ncAlert
) {
}
