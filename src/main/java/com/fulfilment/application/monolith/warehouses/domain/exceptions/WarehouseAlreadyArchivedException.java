package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when attempting to archive a warehouse that is already archived.
 */

@Getter
public class WarehouseAlreadyArchivedException extends RuntimeException {

  private final String businessUnitCode;

  public WarehouseAlreadyArchivedException(String businessUnitCode) {
    super("Warehouse with business unit code '" + businessUnitCode + "' is already archived.");
    this.businessUnitCode = businessUnitCode;
  }
}

