package com.pnc.masters.pcbinventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PcbInventoryRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be 500 characters or less")
        String description,
        @NotBlank(message = "Item type is required")
        String itemType,
        Long manufacturerId,
        @NotBlank(message = "Sheet size is required")
        @Size(max = 200, message = "Sheet size must be 200 characters or less")
        String sheetSize,
        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must be 200 characters or less")
        String location,
        @Size(max = 80, message = "Lead time must be 80 characters or less")
        String leadTime,
        @DecimalMin(value = "0.00", message = "STD price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "STD price must have at most two decimal places")
        BigDecimal stdPrice,
        @NotNull(message = "Re-order level is required")
        @Min(value = 0, message = "Re-order level cannot be negative")
        Integer reorderLevel,
        @NotNull(message = "Re-order quantity is required")
        @Min(value = 0, message = "Re-order quantity cannot be negative")
        Integer reorderQty,
        @Size(max = 500, message = "Alert must be 500 characters or less")
        String alertOnItem,
        String comments,
        @Min(value = 0, message = "Available cannot be negative")
        Integer available,
        @Min(value = 0, message = "Allocated cannot be negative")
        Integer allocated,
        @Min(value = 0, message = "On order cannot be negative")
        Integer onOrder
) {
}
