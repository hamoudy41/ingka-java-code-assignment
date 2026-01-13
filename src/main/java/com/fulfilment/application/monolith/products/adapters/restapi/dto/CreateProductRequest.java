package com.fulfilment.application.monolith.products.adapters.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.math.BigDecimal;

/**
 * Request DTO for creating a new product.
 *
 * <p>Intentionally mirrors the Product JSON shape while keeping validation at the REST boundary.</p>
 */
public record CreateProductRequest(
    @Null(message = "Id was invalidly set on request.")
    Long id,
    @NotBlank(message = "Product Name was not set on request.")
    String name,
    String description,
    BigDecimal price,
    int stock
) {}

