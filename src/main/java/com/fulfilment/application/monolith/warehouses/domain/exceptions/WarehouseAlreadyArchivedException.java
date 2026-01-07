package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

@Getter
public class WarehouseAlreadyArchivedException extends WarehouseDomainException {

  private final String businessUnitCode;

  public WarehouseAlreadyArchivedException(String businessUnitCode) {
    super("Warehouse with business unit code '" + businessUnitCode + "' is already archived.");
    this.businessUnitCode = businessUnitCode;
  }
}

