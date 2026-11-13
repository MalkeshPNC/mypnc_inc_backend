package com.pnc.masters.pcbinventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PcbInventoryTransactionRequest(
        @NotBlank(message = "Transaction type is required")
        String transType,
        String direction,
        Long vendorId,
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most two decimal places")
        BigDecimal price,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        String details,
        @Size(max = 120, message = "Job # or PO # must be 120 characters or less")
        String reference,
        @NotBlank(message = "Initial name is required")
        String initialName
) {
}
