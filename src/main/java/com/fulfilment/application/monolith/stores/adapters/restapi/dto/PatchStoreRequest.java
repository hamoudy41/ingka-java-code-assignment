package com.fulfilment.application.monolith.stores.adapters.restapi.dto;

import jakarta.validation.constraints.Min;

public record PatchStoreRequest(
    String name,
    @Min(value = 0, message = "Store quantityProductsInStock cannot be negative.")
    Integer quantityProductsInStock
) {}

