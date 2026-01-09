package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when the number of warehouses at a location exceeds the limit.
 */

@Getter
public class LocationWarehouseLimitExceededException extends RuntimeException {

  private final String locationIdentifier;
  private final int currentCount;
  private final int maxWarehouses;

  public LocationWarehouseLimitExceededException(String locationIdentifier, int currentCount, int maxWarehouses) {
    super(String.format("Cannot create warehouse at location '%s'. Current count: %d, Maximum allowed: %d.",
        locationIdentifier, currentCount, maxWarehouses));
    this.locationIdentifier = locationIdentifier;
    this.currentCount = currentCount;
    this.maxWarehouses = maxWarehouses;
  }
}

