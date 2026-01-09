package com.fulfilment.application.monolith.location;

/**
 * Exception thrown when location identifier is invalid or blank.
 */

public class InvalidLocationIdentifierException extends RuntimeException {

  public InvalidLocationIdentifierException(String message) {
    super(message);
  }
}


