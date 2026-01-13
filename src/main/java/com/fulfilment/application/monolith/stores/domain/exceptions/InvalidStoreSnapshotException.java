package com.fulfilment.application.monolith.stores.domain.exceptions;

/**
 * Exception thrown when a store snapshot cannot be created due to invalid state.
 *
 * <p>This is treated as an internal error: snapshot creation should only be invoked with
 * valid and persisted store data.</p>
 */
public class InvalidStoreSnapshotException extends RuntimeException {

  public InvalidStoreSnapshotException(String message) {
    super(message);
  }
}

