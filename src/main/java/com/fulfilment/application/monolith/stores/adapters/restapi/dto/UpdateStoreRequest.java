package com.fulfilment.application.monolith.stores.adapters.restapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing store (full replacement).
 */

public record UpdateStoreRequest(
    @NotBlank(message = "Store name is required.")
    String name,
    @Min(value = 0, message = "Store quantityProductsInStock cannot be negative.")
    int quantityProductsInStock
) {}

