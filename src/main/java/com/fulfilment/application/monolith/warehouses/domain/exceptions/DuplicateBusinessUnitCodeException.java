package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

@Getter
public class DuplicateBusinessUnitCodeException extends WarehouseDomainException {

  private final String businessUnitCode;

  public DuplicateBusinessUnitCodeException(String businessUnitCode) {
    super("Warehouse with business unit code '" + businessUnitCode + "' already exists.");
    this.businessUnitCode = businessUnitCode;
  }
}

