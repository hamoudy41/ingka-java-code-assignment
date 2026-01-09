package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;

/**
 * Port interface for resolving location information.
 * Provides access to location constraints and metadata.
 */
public interface LocationResolver {
  
  /**
   * Resolves a location by its unique identifier.
   *
   * @param identifier the location identifier
   * @return the resolved Location with constraints
   */
  Location resolveByIdentifier(String identifier);
}
