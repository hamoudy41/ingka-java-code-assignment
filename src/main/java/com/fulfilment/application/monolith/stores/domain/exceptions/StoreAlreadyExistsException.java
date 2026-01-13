package com.fulfilment.application.monolith.stores.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when attempting to create or rename a store to an already existing name.
 */
@Getter
public class StoreAlreadyExistsException extends RuntimeException {

  private final String name;

  public StoreAlreadyExistsException(String name) {
    super("Store with name '" + name + "' already exists.");
    this.name = name;
  }
}

