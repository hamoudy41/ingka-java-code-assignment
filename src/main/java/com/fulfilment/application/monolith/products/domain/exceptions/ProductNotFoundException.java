package com.fulfilment.application.monolith.products.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a product is not found by its identifier.
 */
@Getter
public class ProductNotFoundException extends RuntimeException {

  private final Long productId;

  public ProductNotFoundException(Long productId) {
    super("Product with id of " + productId + " does not exist.");
    this.productId = productId;
  }
}

