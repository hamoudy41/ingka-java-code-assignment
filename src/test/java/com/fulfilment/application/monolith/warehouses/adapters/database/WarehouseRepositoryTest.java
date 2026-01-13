package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class WarehouseRepositoryTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.EDGE.%");
  }

  @Test
  @DisplayName("Should handle null business unit code when finding warehouse")
  void shouldHandleNullBusinessUnitCode() {
    Warehouse result = warehouseRepository.findByBusinessUnitCode(null);
    assertNull(result, "Then result should be null when searching with null business unit code");
  }

  @Test
  @DisplayName("Should handle empty business unit code when finding warehouse")
  void shouldHandleEmptyBusinessUnitCode() {
    Warehouse result = warehouseRepository.findByBusinessUnitCode("");
    assertNull(result, "Then result should be null when searching with empty business unit code");
  }

  @Test
  @DisplayName("Should handle whitespace-only business unit code when finding warehouse")
  void shouldHandleWhitespaceBusinessUnitCode() {
    Warehouse result = warehouseRepository.findByBusinessUnitCode("   ");
    assertNull(result, "Then result should be null when searching with whitespace-only business unit code");
  }

  @Test
  @DisplayName("Should count zero warehouses for non-existent location")
  void shouldCountZeroForNonExistentLocation() {
    long count = warehouseRepository.countByLocation("NON-EXISTENT-LOCATION");
    assertEquals(0, count, "Then count should be zero for non-existent location");
  }

  @Test
  @DisplayName("Should count zero warehouses for null location")
  void shouldCountZeroForNullLocation() {
    long count = warehouseRepository.countByLocation(null);
    assertEquals(0, count, "Then count should be zero for null location");
  }

  @Test
  @DisplayName("Should exclude archived warehouses from count")
  void shouldExcludeArchivedWarehousesFromCount() {
    String location = "ZWOLLE-002";
    String businessUnitCode1 = "MWH.EDGE.1." + System.currentTimeMillis();
    String businessUnitCode2 = "MWH.EDGE.2." + System.currentTimeMillis();

    Warehouse warehouse1 = createTestWarehouse(businessUnitCode1, location, 30, 10);
    Warehouse warehouse2 = createTestWarehouse(businessUnitCode2, location, 30, 10);
    warehouseRepository.create(warehouse1);
    warehouseRepository.create(warehouse2);

    long countBefore = warehouseRepository.countByLocation(location);
    assertEquals(2, countBefore, "Then both warehouses should be counted before archiving");

    warehouse1.setArchivedAt(ZonedDateTime.now());
    warehouseRepository.update(warehouse1);

    long countAfter = warehouseRepository.countByLocation(location);
    assertEquals(1, countAfter, "Then only active warehouse should be counted after archiving one");
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent warehouse")
  void shouldThrowExceptionWhenUpdatingNonExistentWarehouse() {
    Warehouse warehouse = createTestWarehouse("MWH.EDGE.NONEXISTENT", "ZWOLLE-002", 30, 10);
    
    assertThrows(WarehouseNotFoundException.class, () -> {
      warehouseRepository.update(warehouse);
    }, "Then exception should be thrown when updating non-existent warehouse");
  }

  @Test
  @DisplayName("Should throw exception when removing non-existent warehouse")
  void shouldThrowExceptionWhenRemovingNonExistentWarehouse() {
    Warehouse warehouse = createTestWarehouse("MWH.EDGE.NONEXISTENT", "ZWOLLE-002", 30, 10);
    
    assertThrows(WarehouseNotFoundException.class, () -> {
      warehouseRepository.remove(warehouse);
    }, "Then exception should be thrown when removing non-existent warehouse");
  }

  @Test
  @DisplayName("Should preserve creation timestamp when warehouse already has one")
  void shouldPreserveExistingCreationTimestamp() {
    String businessUnitCode = "MWH.EDGE.TIMESTAMP." + System.currentTimeMillis();
    ZonedDateTime originalTime = ZonedDateTime.now().minusDays(1);
    
    Warehouse warehouse = createTestWarehouse(businessUnitCode, "ZWOLLE-002", 30, 10);
    warehouse.setCreationAt(originalTime);
    
    warehouseRepository.create(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertNotNull(retrieved.getCreationAt(), "Then creation timestamp should be set");
    assertTrue(retrieved.getCreationAt().isAfter(originalTime.minusSeconds(1)), "Then creation timestamp should be preserved");
  }

  @Test
  @DisplayName("Should handle warehouse with null capacity")
  void shouldHandleWarehouseWithNullCapacity() {
    String businessUnitCode = "MWH.EDGE.NULLCAP." + System.currentTimeMillis();
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(null);
    warehouse.setStock(10);
    
    warehouseRepository.create(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertNull(retrieved.getCapacity(), "Then capacity should remain null");
  }

  @Test
  @DisplayName("Should handle warehouse with null stock")
  void shouldHandleWarehouseWithNullStock() {
    String businessUnitCode = "MWH.EDGE.NULLSTOCK." + System.currentTimeMillis();
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(30);
    warehouse.setStock(null);
    
    warehouseRepository.create(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertNull(retrieved.getStock(), "Then stock should remain null");
  }

  @Test
  @DisplayName("Should handle updating warehouse with null archived timestamp")
  void shouldHandleUpdatingWarehouseWithNullArchivedTimestamp() {
    String businessUnitCode = "MWH.EDGE.UPDATE." + System.currentTimeMillis();
    Warehouse warehouse = createTestWarehouse(businessUnitCode, "ZWOLLE-002", 30, 10);
    warehouseRepository.create(warehouse);
    
    warehouse.setCapacity(50);
    warehouse.setArchivedAt(null);
    warehouseRepository.update(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertNull(retrieved.getArchivedAt(), "Then archived timestamp should remain null");
    assertEquals(50, retrieved.getCapacity(), "Then capacity should be updated to 50");
  }

  @Test
  @DisplayName("Should handle zero capacity warehouse")
  void shouldHandleZeroCapacityWarehouse() {
    String businessUnitCode = "MWH.EDGE.ZEROCAP." + System.currentTimeMillis();
    Warehouse warehouse = createTestWarehouse(businessUnitCode, "ZWOLLE-002", 0, 0);
    
    warehouseRepository.create(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertEquals(0, retrieved.getCapacity(), "Then capacity should be zero");
    assertEquals(0, retrieved.getStock(), "Then stock should be zero");
  }

  @Test
  @DisplayName("Should handle very long business unit code")
  void shouldHandleVeryLongBusinessUnitCode() {
    String longCode = "MWH.EDGE." + "A".repeat(200) + "." + System.currentTimeMillis();
    Warehouse warehouse = createTestWarehouse(longCode, "ZWOLLE-002", 30, 10);
    
    warehouseRepository.create(warehouse);
    
    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode(longCode);
    assertNotNull(retrieved, "Then warehouse should be retrieved");
    assertEquals(longCode, retrieved.getBusinessUnitCode(), "Then business unit code should match the long code");
  }

  private Warehouse createTestWarehouse(String businessUnitCode, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation(location);
    warehouse.setCapacity(capacity);
    warehouse.setStock(stock);
    return warehouse;
  }
}
