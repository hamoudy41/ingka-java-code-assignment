package com.fulfilment.application.monolith.stores.domain.exceptions;

/**
 * Exception thrown when legacy system synchronization fails.
 */

public class LegacySyncException extends RuntimeException {

  public LegacySyncException(String message) {
    super(message);
  }

  public LegacySyncException(String message, Throwable cause) {
    super(message, cause);
  }
}

