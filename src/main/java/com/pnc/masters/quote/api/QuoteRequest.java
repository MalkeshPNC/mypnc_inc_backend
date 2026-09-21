package com.pnc.masters.quote.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * assyQuotePerson and pcbQuotePerson are deliberately absent: the server stamps
 * each one when its matching status changes.
 */
public record QuoteRequest(
        @NotBlank(message = "quoteNumber is required") String quoteNumber,
        String quoteType,
        String projectNumber,
        @NotNull(message = "createDate is required") LocalDate createDate,
        LocalDate submitDate,
        Long custId,
        String customerName,
        Long contId,
        String contactName,
        String customerRfq,
        Long ncId,
        String ncNumber,
        String assyNumber,
        String pcbaRevision,
        String pcbNumber,
        String pcbRevision,
        String assyQuoteStatus,
        String pcbQuoteNumber,
        String pcbQuoteStatus,
        String array,
        @Digits(integer = 4, fraction = 2, message = "commissionPercentage must have up to 4 integer digits and 2 decimals")
        BigDecimal commissionPercentage,
        String internalNote1,
        String internalNote2,
        String notesToCustomer,
        String otherNreCharges,
        LocalDate receivedDate,
        Boolean pncNotes,
        Boolean laborOnly,
        Boolean partsScheduled,
        Boolean feedback,
        Boolean itarc,
        Boolean berryc,
        Boolean samsReview,
        String pcbaPlant,
        String pcbOrigin,
        String status,
        Boolean faiReqPnc,
        BigDecimal faiPcbWo,
        BigDecimal faiPcbaWo,
        String faiHeadingPnc,
        Boolean faiReqAs9102,
        BigDecimal faiPcbWith,
        BigDecimal faiPcbaWith,
        String faiHeadingAs9102,
        @Valid List<QuoteQuantityRequest> quantities
) {
}
