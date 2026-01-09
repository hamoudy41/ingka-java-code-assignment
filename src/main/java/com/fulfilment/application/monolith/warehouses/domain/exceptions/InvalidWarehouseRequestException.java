package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Exception thrown for invalid warehouse request data.
 */

public class InvalidWarehouseRequestException extends RuntimeException {

  public InvalidWarehouseRequestException(String message) {
    super(message);
  }

  public InvalidWarehouseRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}

