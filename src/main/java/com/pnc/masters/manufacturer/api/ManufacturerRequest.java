package com.pnc.masters.manufacturer.api;

import com.pnc.masters.party.api.PartyInput;

public record ManufacturerRequest(
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
