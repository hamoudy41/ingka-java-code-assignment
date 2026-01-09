package com.fulfilment.application.monolith.stores.domain.exceptions;

/**
 * Exception thrown for invalid store request data.
 */

public class InvalidStoreRequestException extends RuntimeException {

  public InvalidStoreRequestException(String message) {
    super(message);
  }

  public InvalidStoreRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}

