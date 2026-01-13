package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.Location;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LocationGatewayTest {

  @Inject
  LocationGateway locationGateway;

  @Test
  @DisplayName("resolveByIdentifier should return matching Location for existing identifier")
  public void testWhenResolveExistingLocationShouldReturn() {
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertEquals("ZWOLLE-001", location.identification(), "Then location identifier should match");
  }

  @Test
  @DisplayName("resolveByIdentifier should throw when identifier does not exist")
  public void testWhenResolveNonExistingLocationShouldThrowNotFound() {
    assertThrows(
        LocationNotFoundException.class,
        () -> locationGateway.resolveByIdentifier("NON-EXISTENT-ID"),
        "Then exception should be thrown when identifier does not exist");
  }

  @Test
  @DisplayName("resolveByIdentifier should reject null, empty, or blank identifiers with specific exception")
  public void testWhenIdentifierIsNullOrBlankShouldThrowDomainException() {
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier(null),
        "Then exception should be thrown for null identifier");
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier(""),
        "Then exception should be thrown for empty identifier");
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier("   "),
        "Then exception should be thrown for blank identifier");
    assertThrows(
        InvalidLocationIdentifierException.class,
        () -> locationGateway.resolveByIdentifier("\t\n\r"),
        "Then exception should be thrown for whitespace-only identifier");
  }

  @Test
  @DisplayName("Should be case-sensitive when resolving location")
  void shouldBeCaseSensitiveWhenResolvingLocation() {
    assertThrows(LocationNotFoundException.class, () -> {
      locationGateway.resolveByIdentifier("zwolle-001");
    }, "Then exception should be thrown for case-sensitive mismatch");
  }

  @Test
  @DisplayName("Should handle location identifier with special characters")
  void shouldHandleLocationIdentifierWithSpecialCharacters() {
    assertThrows(LocationNotFoundException.class, () -> {
      locationGateway.resolveByIdentifier("ZWOLLE-001!");
    }, "Then exception should be thrown for identifier with special characters");
  }

  @Test
  @DisplayName("Should handle very long location identifier")
  void shouldHandleVeryLongLocationIdentifier() {
    String longIdentifier = "A".repeat(1000);
    assertThrows(LocationNotFoundException.class, () -> {
      locationGateway.resolveByIdentifier(longIdentifier);
    }, "Then exception should be thrown for very long identifier");
  }

  @Test
  @DisplayName("Should resolve all predefined locations")
  void shouldResolveAllPredefinedLocations() {
    String[] locations = {
        "ZWOLLE-001", "ZWOLLE-002",
        "AMSTERDAM-001", "AMSTERDAM-002",
        "TILBURG-001",
        "HELMOND-001",
        "EINDHOVEN-001",
        "VETSBY-001"
    };

    for (String locationId : locations) {
      Location location = locationGateway.resolveByIdentifier(locationId);
      assertEquals(locationId, location.identification(), "Then location " + locationId + " should be resolved correctly");
    }
  }

  @Test
  @DisplayName("Should handle location identifier with leading/trailing spaces")
  void shouldHandleLocationIdentifierWithSpaces() {
    assertThrows(LocationNotFoundException.class, () -> {
      locationGateway.resolveByIdentifier(" ZWOLLE-001 ");
    }, "Then exception should be thrown for identifier with leading/trailing spaces");
  }
}

