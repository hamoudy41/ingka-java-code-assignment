package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;

public interface LocationResolver {
  /**
   * Resolves a {@link Location} from its identifier.
   *
   * @param identifier non-null and non-blank location identifier
   * @return the resolved {@link Location}
   * @throws com.fulfilment.application.monolith.location.InvalidLocationIdentifierException if the
   *     identifier is syntactically invalid (e.g. null or blank)
   * @throws com.fulfilment.application.monolith.location.LocationNotFoundException if no location
   *     exists for the given identifier
   */
  Location resolveByIdentifier(String identifier);
}
