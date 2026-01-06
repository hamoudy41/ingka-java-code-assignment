package com.fulfilment.application.monolith.location;

public class LocationNotFoundException extends RuntimeException {

  public LocationNotFoundException(String identifier) {
    super("No Location found for identifier '" + identifier + "'");
  }
}


