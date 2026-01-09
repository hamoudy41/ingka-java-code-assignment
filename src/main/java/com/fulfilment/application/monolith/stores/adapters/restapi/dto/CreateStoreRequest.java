package com.fulfilment.application.monolith.stores.adapters.restapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

/**
 * Request DTO for creating a new store.
 */

public record CreateStoreRequest(
    @Null(message = "Id was invalidly set on request.")
    Long id,
    @NotBlank(message = "Store name is required.")
    String name,
    @Min(value = 0, message = "Store quantityProductsInStock cannot be negative.")
    int quantityProductsInStock
) {}

