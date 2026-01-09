package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when warehouse capacity exceeds location maximum.
 */

@Getter
public class LocationCapacityExceededException extends RuntimeException {

  private final int requestedCapacity;
  private final int maxCapacity;
  private final String locationIdentifier;

  public LocationCapacityExceededException(String locationIdentifier, int requestedCapacity, int maxCapacity) {
    super(String.format("Warehouse capacity %d exceeds maximum capacity %d for location '%s'.",
        requestedCapacity, maxCapacity, locationIdentifier));
    this.locationIdentifier = locationIdentifier;
    this.requestedCapacity = requestedCapacity;
    this.maxCapacity = maxCapacity;
  }
}

