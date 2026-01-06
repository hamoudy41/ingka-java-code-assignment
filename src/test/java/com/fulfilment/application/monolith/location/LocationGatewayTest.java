package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private final LocationGateway locationGateway = new LocationGateway();

  @Test
  @DisplayName("resolveByIdentifier should return matching Location for existing identifier")
  public void testWhenResolveExistingLocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  @DisplayName("resolveByIdentifier should throw when identifier does not exist")
  public void testWhenResolveNonExistingLocationShouldThrowNotFound() {
    assertThrows(
        LocationNotFoundException.class,
        () -> locationGateway.resolveByIdentifier("NON-EXISTENT-ID"));
  }

  @Test
  @DisplayName("resolveByIdentifier should reject null or blank identifiers with specific exception")
  public void testWhenIdentifierIsNullOrBlankShouldThrowDomainException() {
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier(null));
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier("   "));
  }
}

