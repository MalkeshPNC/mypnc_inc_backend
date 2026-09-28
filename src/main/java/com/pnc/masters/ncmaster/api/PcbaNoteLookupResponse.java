package com.pnc.masters.ncmaster.api;

public record PcbaNoteLookupResponse(
        boolean found,
        Long pcbaNoteId,
        String pcbaPartNumber,
        String pcbaNotes
) {
}
