package com.fulfilment.application.monolith.products.adapters.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Request DTO for updating an existing product.
 */
public record UpdateProductRequest(
    @NotBlank(message = "Product Name was not set on request.")
    String name,
    String description,
    BigDecimal price,
    int stock
) {}

