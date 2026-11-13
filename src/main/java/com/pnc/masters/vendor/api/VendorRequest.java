package com.pnc.masters.vendor.api;

import com.pnc.masters.party.api.PartyInput;

public record VendorRequest(
        String code,
        String name,
        Long typeEntryId,
        Long subtypeEntryId,
        String contactName,
        String contactAddress,
        String contactEmail,
        String contactPhone
) implements PartyInput {
}
