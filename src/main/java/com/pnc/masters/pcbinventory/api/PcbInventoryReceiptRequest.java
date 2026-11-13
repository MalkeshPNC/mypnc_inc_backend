package com.pnc.masters.pcbinventory.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PcbInventoryReceiptRequest(
        @NotNull(message = "Vendor is required")
        Long vendorId,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        String details,
        @NotBlank(message = "Receipt number is required")
        @Size(max = 120, message = "Receipt number must be 120 characters or less")
        String receiptNo
) {
}
