package com.pnc.masters.party.api;

import java.time.LocalDateTime;

public record PartyResponse(
        Long id,
        String code,
        String name,
        Long typeEntryId,
        String typeName,
        Long subtypeEntryId,
        String subtypeName,
        String contactName,
        String contactAddress,
        String contactEmail,
        String contactPhone,
        Long createdByUserId,
        String createdBy,
        LocalDateTime createdDate,
        Long updatedByUserId,
        String updatedBy,
        LocalDateTime updatedDate
) {
}
