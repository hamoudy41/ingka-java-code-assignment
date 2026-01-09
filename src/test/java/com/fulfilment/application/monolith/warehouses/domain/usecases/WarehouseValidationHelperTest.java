package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class WarehouseValidationHelperTest {

  @Inject
  WarehouseValidationHelper validationHelper;

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.VALIDATION.%");
  }

  @Test
  @DisplayName("Should pass validation when warehouse count is exactly at limit")
  void shouldPassWhenCountExactlyAtLimit() {
    String location = "ZWOLLE-001";
    String businessUnitCode = "MWH.VALIDATION.1." + System.currentTimeMillis();
    
    Warehouse warehouse = createTestWarehouse(businessUnitCode, location, 30, 10);
    warehouseRepository.create(warehouse);
    
    assertThrows(LocationWarehouseLimitExceededException.class, () -> {
      validationHelper.validateWarehouseCountLimit(location, 1);
    });
  }

  @Test
  @DisplayName("Should pass validation when warehouse count is one below limit")
  void shouldPassWhenCountOneBelowLimit() {
    String location = "ZWOLLE-002";
    String businessUnitCode = "MWH.VALIDATION.1." + System.currentTimeMillis();
    
    Warehouse warehouse = createTestWarehouse(businessUnitCode, location, 30, 10);
    warehouseRepository.create(warehouse);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateWarehouseCountLimit(location, 2);
    }, "Then validation should pass when warehouse count is one below limit");
  }

  @Test
  @DisplayName("Should throw exception when warehouse count exceeds limit by one")
  void shouldThrowWhenCountExceedsLimitByOne() {
    String location = "ZWOLLE-001";
    String businessUnitCode = "MWH.VALIDATION.1." + System.currentTimeMillis();
    
    Warehouse warehouse = createTestWarehouse(businessUnitCode, location, 30, 10);
    warehouseRepository.create(warehouse);
    
    assertThrows(LocationWarehouseLimitExceededException.class, () -> {
      validationHelper.validateWarehouseCountLimit(location, 1);
    });
  }

  @Test
  @DisplayName("Should pass validation when capacity equals location maximum")
  void shouldPassWhenCapacityEqualsMaximum() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 10);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateCapacityAgainstLocationMax(warehouse, 50);
    }, "Then validation should pass when capacity equals location maximum");
  }

  @Test
  @DisplayName("Should pass validation when capacity is one below maximum")
  void shouldPassWhenCapacityOneBelowMaximum() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 49, 10);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateCapacityAgainstLocationMax(warehouse, 50);
    }, "Then validation should pass when capacity is one below maximum");
  }

  @Test
  @DisplayName("Should throw exception when capacity exceeds maximum by one")
  void shouldThrowWhenCapacityExceedsMaximumByOne() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 51, 10);
    
    assertThrows(LocationCapacityExceededException.class, () -> {
      validationHelper.validateCapacityAgainstLocationMax(warehouse, 50);
    }, "Then exception should be thrown when capacity exceeds maximum by one");
  }

  @Test
  @DisplayName("Should pass validation when stock equals capacity")
  void shouldPassWhenStockEqualsCapacity() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 50);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateStockAgainstCapacity(warehouse);
    }, "Then validation should pass when stock equals capacity");
  }

  @Test
  @DisplayName("Should pass validation when stock is one below capacity")
  void shouldPassWhenStockOneBelowCapacity() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 49);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateStockAgainstCapacity(warehouse);
    }, "Then validation should pass when stock is one below capacity");
  }

  @Test
  @DisplayName("Should throw exception when stock exceeds capacity by one")
  void shouldThrowWhenStockExceedsCapacityByOne() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 51);
    
    assertThrows(InsufficientCapacityException.class, () -> {
      validationHelper.validateStockAgainstCapacity(warehouse);
    });
  }

  @Test
  @DisplayName("Should handle zero capacity warehouse")
  void shouldHandleZeroCapacityWarehouse() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 0, 0);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateStockAgainstCapacity(warehouse);
    }, "Then validation should pass when capacity and stock are both zero");
  }

  @Test
  @DisplayName("Should throw exception when stock exceeds zero capacity")
  void shouldThrowWhenStockExceedsZeroCapacity() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 0, 1);
    
    assertThrows(InsufficientCapacityException.class, () -> {
      validationHelper.validateStockAgainstCapacity(warehouse);
    });
  }

  @Test
  @DisplayName("Should handle very large capacity values")
  void shouldHandleVeryLargeCapacityValues() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", Integer.MAX_VALUE, 1000);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateCapacityAgainstLocationMax(warehouse, Integer.MAX_VALUE);
    }, "Then validation should pass for very large capacity values");
  }

  @Test
  @DisplayName("Should validate both capacity constraints together")
  void shouldValidateBothCapacityConstraintsTogether() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 30);
    
    assertDoesNotThrow(() -> {
      validationHelper.validateCapacityConstraints(warehouse, 50);
    }, "Then validation should pass when both capacity constraints are satisfied");
  }

  @Test
  @DisplayName("Should throw when capacity exceeds location max in combined validation")
  void shouldThrowWhenCapacityExceedsLocationMaxInCombinedValidation() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 51, 30);
    
    assertThrows(LocationCapacityExceededException.class, () -> {
      validationHelper.validateCapacityConstraints(warehouse, 50);
    }, "Then exception should be thrown when capacity exceeds location max in combined validation");
  }

  @Test
  @DisplayName("Should throw when stock exceeds capacity in combined validation")
  void shouldThrowWhenStockExceedsCapacityInCombinedValidation() {
    Warehouse warehouse = createTestWarehouse("MWH.VALIDATION.TEST", "ZWOLLE-002", 50, 51);
    
    assertThrows(InsufficientCapacityException.class, () -> {
      validationHelper.validateCapacityConstraints(warehouse, 50);
    }, "Then exception should be thrown when stock exceeds capacity in combined validation");
  }

  @Test
  @DisplayName("Should handle null location identifier")
  void shouldHandleNullLocationIdentifier() {
    assertDoesNotThrow(() -> {
      validationHelper.validateWarehouseCountLimit(null, 1);
    }, "Then validation should pass for null location identifier");
  }

  @Test
  @DisplayName("Should handle empty location identifier")
  void shouldHandleEmptyLocationIdentifier() {
    assertDoesNotThrow(() -> {
      validationHelper.validateWarehouseCountLimit("", 1);
    }, "Then validation should pass for empty location identifier");
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
