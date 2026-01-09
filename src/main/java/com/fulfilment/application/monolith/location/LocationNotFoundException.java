package com.fulfilment.application.monolith.location;

/**
 * Exception thrown when a location is not found by its identifier.
 */
public class LocationNotFoundException extends RuntimeException {

  public LocationNotFoundException(String identifier) {
    super("No Location found for identifier '" + identifier + "'");
  }
}


