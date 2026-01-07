package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class InvalidWarehouseRequestException extends WarehouseDomainException {

  public InvalidWarehouseRequestException(String message) {
    super(message);
  }

  public InvalidWarehouseRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}

