package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.location.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseRequestException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.StockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class CreateWarehouseUseCaseTest {

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.%");
  }

  @Test
  @DisplayName("Should create warehouse successfully when all validations pass")
  void shouldCreateWarehouseSuccessfully() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(45);
    warehouse.setStock(10);

    createWarehouseUseCase.create(warehouse);

    assertNotNull(warehouse.getCreationAt(), "Then creation timestamp should be set");
  }

  @Test
  @DisplayName("Should throw DuplicateBusinessUnitCodeException when business unit code already exists")
  void shouldThrowDuplicateBusinessUnitCodeException() {
    String businessUnitCode = "MWH.DUPLICATE." + System.currentTimeMillis();
    
    Warehouse first = new Warehouse();
    first.setBusinessUnitCode(businessUnitCode);
    first.setLocation("HELMOND-001");
    first.setCapacity(40);
    first.setStock(10);
    createWarehouseUseCase.create(first);

    Warehouse duplicate = new Warehouse();
    duplicate.setBusinessUnitCode(businessUnitCode);
    duplicate.setLocation("HELMOND-001");
    duplicate.setCapacity(40);
    duplicate.setStock(10);

    assertThrows(DuplicateBusinessUnitCodeException.class, () -> {
      createWarehouseUseCase.create(duplicate);
    }, "Then exception should be thrown when creating warehouse with duplicate business unit code");
  }

  @Test
  @DisplayName("Should throw LocationNotFoundException when location does not exist")
  void shouldThrowLocationNotFoundException() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("INVALID-LOCATION");
    warehouse.setCapacity(30);
    warehouse.setStock(10);

    assertThrows(LocationNotFoundException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    }, "Then exception should be thrown when location does not exist");
  }

  @Test
  @DisplayName("Should throw LocationCapacityExceededException when capacity exceeds location max")
  void shouldThrowLocationCapacityExceededException() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(55);
    warehouse.setStock(10);

    assertThrows(LocationCapacityExceededException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    }, "Then exception should be thrown when capacity exceeds location maximum");
  }

  @Test
  @DisplayName("Should throw InsufficientCapacityException when stock exceeds capacity")
  void shouldThrowInsufficientCapacityException() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(30);
    warehouse.setStock(35);

    assertThrows(InsufficientCapacityException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    }, "Then exception should be thrown when stock exceeds capacity");
  }

  @Test
  @DisplayName("Should throw exception when warehouse count limit is exactly reached")
  void shouldThrowExceptionWhenWarehouseCountLimitReached() {
    String location = "ZWOLLE-001";
    
    Warehouse first = new Warehouse();
    first.setBusinessUnitCode("MWH.TEST.1." + System.currentTimeMillis());
    first.setLocation(location);
    first.setCapacity(30);
    first.setStock(10);
    createWarehouseUseCase.create(first);

    Warehouse second = new Warehouse();
    second.setBusinessUnitCode("MWH.TEST.2." + System.currentTimeMillis());
    second.setLocation(location);
    second.setCapacity(30);
    second.setStock(10);

    assertThrows(com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException.class, () -> {
      createWarehouseUseCase.create(second);
    }, "Then exception should be thrown when warehouse count limit is reached");
  }

  @Test
  @DisplayName("Should handle warehouse with zero capacity and zero stock")
  void shouldHandleZeroCapacityAndStock() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(0);
    warehouse.setStock(0);

    createWarehouseUseCase.create(warehouse);
    assertNotNull(warehouse.getCreationAt(), "Then warehouse should be created with zero capacity and stock");
  }

  @Test
  @DisplayName("Should handle warehouse with capacity exactly at location maximum")
  void shouldHandleCapacityAtLocationMaximum() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(50);
    warehouse.setStock(10);

    createWarehouseUseCase.create(warehouse);
  }

  @Test
  @DisplayName("Should handle warehouse with stock exactly at capacity")
  void shouldHandleStockAtCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.TEST." + System.currentTimeMillis());
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(30);
    warehouse.setStock(30);

    createWarehouseUseCase.create(warehouse);
    assertNotNull(warehouse.getCreationAt(), "Then warehouse should be created when stock equals capacity");
  }

  @Test
  @DisplayName("WarehouseNotFoundException should create with identifier")
  void shouldCreateWarehouseNotFoundException() {
    String identifier = "MWH.TEST.001";
    WarehouseNotFoundException exception = new WarehouseNotFoundException(identifier);
    
    assertEquals(identifier, exception.getIdentifier(), "Then identifier should match");
    assertTrue(exception.getMessage().contains(identifier), "Then message should contain identifier");
  }

  @Test
  @DisplayName("InsufficientCapacityException should create with capacity and stock")
  void shouldCreateInsufficientCapacityException() {
    int capacity = 50;
    int requiredStock = 75;
    InsufficientCapacityException exception = new InsufficientCapacityException(capacity, requiredStock);
    
    assertEquals(capacity, exception.getCapacity(), "Then capacity should match");
    assertEquals(requiredStock, exception.getRequiredStock(), "Then required stock should match");
    assertTrue(exception.getMessage().contains("insufficient"), "Then message should contain 'insufficient'");
  }

  @Test
  @DisplayName("DuplicateBusinessUnitCodeException should create with code")
  void shouldCreateDuplicateBusinessUnitCodeException() {
    String code = "MWH.DUPLICATE.001";
    DuplicateBusinessUnitCodeException exception = new DuplicateBusinessUnitCodeException(code);
    
    assertEquals(code, exception.getBusinessUnitCode(), "Then business unit code should match");
    assertTrue(exception.getMessage().contains("already exists"), "Then message should contain 'already exists'");
  }

  @Test
  @DisplayName("LocationCapacityExceededException should create with location and capacities")
  void shouldCreateLocationCapacityExceededException() {
    String location = "ZWOLLE-001";
    int requestedCapacity = 100;
    int maxCapacity = 75;
    LocationCapacityExceededException exception = 
        new LocationCapacityExceededException(location, requestedCapacity, maxCapacity);
    
    assertEquals(location, exception.getLocationIdentifier(), "Then location should match");
    assertEquals(requestedCapacity, exception.getRequestedCapacity(), "Then requested capacity should match");
    assertEquals(maxCapacity, exception.getMaxCapacity(), "Then max capacity should match");
  }

  @Test
  @DisplayName("LocationWarehouseLimitExceededException should create with location and counts")
  void shouldCreateLocationWarehouseLimitExceededException() {
    String location = "AMSTERDAM-001";
    int currentCount = 3;
    int maxWarehouses = 3;
    LocationWarehouseLimitExceededException exception = 
        new LocationWarehouseLimitExceededException(location, currentCount, maxWarehouses);
    
    assertEquals(location, exception.getLocationIdentifier(), "Then location should match");
    assertEquals(currentCount, exception.getCurrentCount(), "Then current count should match");
    assertEquals(maxWarehouses, exception.getMaxWarehouses(), "Then max warehouses should match");
  }

  @Test
  @DisplayName("StockMismatchException should create with existing and new stock")
  void shouldCreateStockMismatchException() {
    int existingStock = 50;
    int newStock = 75;
    StockMismatchException exception = new StockMismatchException(existingStock, newStock);
    
    assertEquals(existingStock, exception.getExistingStock(), "Then existing stock should match");
    assertEquals(newStock, exception.getNewStock(), "Then new stock should match");
    assertTrue(exception.getMessage().contains("mismatch"), "Then message should contain 'mismatch'");
  }

  @Test
  @DisplayName("WarehouseAlreadyArchivedException should create with business unit code")
  void shouldCreateWarehouseAlreadyArchivedException() {
    String code = "MWH.ARCHIVE.001";
    WarehouseAlreadyArchivedException exception = new WarehouseAlreadyArchivedException(code);
    
    assertEquals(code, exception.getBusinessUnitCode(), "Then business unit code should match");
    assertTrue(exception.getMessage().contains("already archived"), "Then message should contain 'already archived'");
  }

  @Test
  @DisplayName("InvalidWarehouseRequestException should create with message")
  void shouldCreateInvalidWarehouseRequestException() {
    String message = "Invalid warehouse request";
    InvalidWarehouseRequestException exception = new InvalidWarehouseRequestException(message);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
    assertNull(exception.getCause(), "Then cause should be null");
  }

  @Test
  @DisplayName("InvalidWarehouseRequestException should create with message and cause")
  void shouldCreateInvalidWarehouseRequestExceptionWithCause() {
    String message = "Invalid request";
    Throwable cause = new IllegalArgumentException("Cause");
    InvalidWarehouseRequestException exception = new InvalidWarehouseRequestException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
    assertEquals(cause, exception.getCause(), "Then cause should match");
  }
}
