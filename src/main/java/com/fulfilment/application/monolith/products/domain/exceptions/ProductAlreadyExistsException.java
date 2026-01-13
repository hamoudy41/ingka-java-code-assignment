package com.fulfilment.application.monolith.products.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when attempting to create or rename a product to an already existing name.
 */
@Getter
public class ProductAlreadyExistsException extends RuntimeException {

  private final String name;

  public ProductAlreadyExistsException(String name) {
    super("Product with name '" + name + "' already exists.");
    this.name = name;
  }
}

