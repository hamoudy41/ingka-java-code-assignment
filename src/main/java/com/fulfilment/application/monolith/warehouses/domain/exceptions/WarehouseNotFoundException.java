package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a warehouse is not found by its identifier.
 */
@Getter
public class WarehouseNotFoundException extends RuntimeException {

  private final String identifier;

  public WarehouseNotFoundException(String identifier) {
    super("Warehouse with identifier '" + identifier + "' does not exist.");
    this.identifier = identifier;
  }
}

