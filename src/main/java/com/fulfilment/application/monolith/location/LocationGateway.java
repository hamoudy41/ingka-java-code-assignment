package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.jbosslog.JBossLog;

/**
 * Gateway for resolving location information.
 * Provides access to location data including maximum warehouse count and capacity.
 */
@ApplicationScoped
@JBossLog
public class LocationGateway implements LocationResolver {

  private static final List<Location> locations = new ArrayList<>();

  static {
    locations.add(new Location("ZWOLLE-001", 1, 40));
    locations.add(new Location("ZWOLLE-002", 2, 50));
    locations.add(new Location("AMSTERDAM-001", 5, 100));
    locations.add(new Location("AMSTERDAM-002", 3, 75));
    locations.add(new Location("TILBURG-001", 1, 40));
    locations.add(new Location("HELMOND-001", 1, 45));
    locations.add(new Location("EINDHOVEN-001", 2, 70));
    locations.add(new Location("VETSBY-001", 1, 90));
  }

  /**
   * Resolves a location by its identifier.
   *
   * @param identifier the location identifier
   * @return the Location record with max warehouses and capacity
   * @throws InvalidLocationIdentifierException if identifier is null or blank
   * @throws LocationNotFoundException if location is not found
   */
  @Override
  public Location resolveByIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      log.warn("Attempted to resolve Location with null or blank identifier");
      throw new InvalidLocationIdentifierException("Location identifier must be provided");
    }

    return locations.stream()
        .filter(location -> identifier.equals(location.identification()))
        .findFirst()
        .map(location -> {
          log.debugf("Resolved Location for identifier '%s' -> maxWarehouses=%d, maxCapacity=%d",
              identifier, location.maxNumberOfWarehouses(), location.maxCapacity());
          return location;
        })
        .orElseThrow(() -> {
          log.warnf("No Location found for identifier '%s'", identifier);
          return new LocationNotFoundException(identifier);
        });
  }
}
