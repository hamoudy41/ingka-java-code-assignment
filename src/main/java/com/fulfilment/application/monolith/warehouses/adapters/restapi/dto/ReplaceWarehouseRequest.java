package com.fulfilment.application.monolith.warehouses.adapters.restapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for replacing an existing warehouse.
 */

public record ReplaceWarehouseRequest(
    @NotBlank(message = "Location is required.")
    String location,
    @NotNull(message = "Capacity is required.")
    @Min(value = 1, message = "Capacity must be at least 1.")
    Integer capacity,
    @NotNull(message = "Stock is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    Integer stock
) {}

