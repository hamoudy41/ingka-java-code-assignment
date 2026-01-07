package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

@Getter
public class WarehouseNotFoundException extends WarehouseDomainException {

  private final String identifier;

  public WarehouseNotFoundException(String identifier) {
    super("Warehouse with identifier '" + identifier + "' does not exist.");
    this.identifier = identifier;
  }
}

