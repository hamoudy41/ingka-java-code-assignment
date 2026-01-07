package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.StockMismatchException;
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
class ReplaceWarehouseUseCaseTest {

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Inject
  ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.%");
  }

  @Test
  @DisplayName("Should replace warehouse successfully when all validations pass")
  void shouldReplaceWarehouseSuccessfully() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    
    Warehouse existing = new Warehouse();
    existing.setBusinessUnitCode(businessUnitCode);
    existing.setLocation("AMSTERDAM-002");
    existing.setCapacity(60);
    existing.setStock(20);
    createWarehouseUseCase.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.setBusinessUnitCode(businessUnitCode);
    replacement.setLocation("AMSTERDAM-002");
    replacement.setCapacity(70);
    replacement.setStock(20);

    replaceWarehouseUseCase.replace(replacement);
  }

  @Test
  @DisplayName("Should throw WarehouseNotFoundException when warehouse does not exist")
  void shouldThrowWarehouseNotFoundException() {
    Warehouse replacement = new Warehouse();
    replacement.setBusinessUnitCode("MWH.NONEXISTENT");
    replacement.setLocation("AMSTERDAM-002");
    replacement.setCapacity(30);
    replacement.setStock(10);

    assertThrows(WarehouseNotFoundException.class, () -> {
      replaceWarehouseUseCase.replace(replacement);
    });
  }

  @Test
  @DisplayName("Should throw LocationCapacityExceededException when capacity exceeds location max")
  void shouldThrowLocationCapacityExceededException() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    
    Warehouse existing = new Warehouse();
    existing.setBusinessUnitCode(businessUnitCode);
    existing.setLocation("AMSTERDAM-002");
    existing.setCapacity(60);
    existing.setStock(20);
    createWarehouseUseCase.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.setBusinessUnitCode(businessUnitCode);
    replacement.setLocation("AMSTERDAM-002");
    replacement.setCapacity(80);
    replacement.setStock(20);

    assertThrows(LocationCapacityExceededException.class, () -> {
      replaceWarehouseUseCase.replace(replacement);
    });
  }

  @Test
  @DisplayName("Should throw InsufficientCapacityException when new capacity is less than existing stock")
  void shouldThrowInsufficientCapacityException() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    
    Warehouse existing = new Warehouse();
    existing.setBusinessUnitCode(businessUnitCode);
    existing.setLocation("AMSTERDAM-002");
    existing.setCapacity(60);
    existing.setStock(20);
    createWarehouseUseCase.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.setBusinessUnitCode(businessUnitCode);
    replacement.setLocation("AMSTERDAM-002");
    replacement.setCapacity(15);
    replacement.setStock(20);

    assertThrows(InsufficientCapacityException.class, () -> {
      replaceWarehouseUseCase.replace(replacement);
    });
  }

  @Test
  @DisplayName("Should throw StockMismatchException when new stock does not match existing stock")
  void shouldThrowStockMismatchException() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    
    Warehouse existing = new Warehouse();
    existing.setBusinessUnitCode(businessUnitCode);
    existing.setLocation("AMSTERDAM-002");
    existing.setCapacity(60);
    existing.setStock(20);
    createWarehouseUseCase.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.setBusinessUnitCode(businessUnitCode);
    replacement.setLocation("AMSTERDAM-002");
    replacement.setCapacity(70);
    replacement.setStock(25);

    assertThrows(StockMismatchException.class, () -> {
      replaceWarehouseUseCase.replace(replacement);
    });
  }
}
