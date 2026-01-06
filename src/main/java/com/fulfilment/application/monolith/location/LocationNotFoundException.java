package com.fulfilment.application.monolith.location;

/**
 * Thrown when a location with a syntactically valid identifier cannot be found in the configured
 * locations.
 */
public class LocationNotFoundException extends RuntimeException {

  public LocationNotFoundException(String identifier) {
    super("No Location found for identifier '" + identifier + "'");
  }
}


