package com.fulfilment.application.monolith.products.adapters.restapi.dto;

import java.math.BigDecimal;

/**
 * Response DTO for product operations.
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    int stock
) {}

