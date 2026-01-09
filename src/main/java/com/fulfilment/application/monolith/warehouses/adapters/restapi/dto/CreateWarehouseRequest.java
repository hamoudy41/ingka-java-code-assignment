package com.fulfilment.application.monolith.warehouses.adapters.restapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Request DTO for creating a new warehouse.
 */

public record CreateWarehouseRequest(
    @Null(message = "Id was invalidly set on request.")
    String id,
    @NotBlank(message = "Business unit code is required.")
    String businessUnitCode,
    @NotBlank(message = "Location is required.")
    String location,
    @NotNull(message = "Capacity is required.")
    @Min(value = 1, message = "Capacity must be at least 1.")
    Integer capacity,
    @NotNull(message = "Stock is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    Integer stock
) {}

