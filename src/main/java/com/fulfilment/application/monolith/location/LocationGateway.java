package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

public class LocationGateway implements LocationResolver {

  private static final Logger LOGGER = Logger.getLogger(LocationGateway.class);

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

  @Override
  public Location resolveByIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      LOGGER.warn("Attempted to resolve Location with null or blank identifier");
      throw new InvalidLocationIdentifierException("Location identifier must be provided");
    }

    for (Location location : locations) {
      if (identifier.equals(location.identification)) {
        LOGGER.debugf(
            "Resolved Location for identifier '%s' -> maxWarehouses=%d, maxCapacity=%d",
            identifier, location.maxNumberOfWarehouses, location.maxCapacity);
        return location;
      }
    }

    LOGGER.warnf("No Location found for identifier '%s'", identifier);
    throw new LocationNotFoundException(identifier);
  }
}
