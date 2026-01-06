package com.fulfilment.application.monolith.location;

/**
 * Thrown when a location identifier is syntactically invalid (e.g. null or blank) 
 */
public class InvalidLocationIdentifierException extends RuntimeException {

  public InvalidLocationIdentifierException(String message) {
    super(message);
  }
}


