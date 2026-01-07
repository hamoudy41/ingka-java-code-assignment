package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
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

    assertNotNull(warehouse.getCreationAt());
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
    });
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
    });
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
    });
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
    });
  }
}
