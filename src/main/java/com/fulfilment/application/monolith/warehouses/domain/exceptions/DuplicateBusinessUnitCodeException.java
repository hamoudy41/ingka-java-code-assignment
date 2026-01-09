package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when attempting to create a warehouse with a business unit code that already exists.
 */

@Getter
public class DuplicateBusinessUnitCodeException extends RuntimeException {

  private final String businessUnitCode;

  public DuplicateBusinessUnitCodeException(String businessUnitCode) {
    super("Warehouse with business unit code '" + businessUnitCode + "' already exists.");
    this.businessUnitCode = businessUnitCode;
  }
}

