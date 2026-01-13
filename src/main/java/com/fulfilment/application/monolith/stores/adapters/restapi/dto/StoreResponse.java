package com.fulfilment.application.monolith.stores.adapters.restapi.dto;

/**
 * Response DTO for store operations.
 */
public record StoreResponse(
    Long id,
    String name,
    int quantityProductsInStock,
    Long version
) {}

