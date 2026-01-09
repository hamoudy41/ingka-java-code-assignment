package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
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
class ArchiveWarehouseUseCaseTest {

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Inject
  ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.%");
  }

  @Test
  @DisplayName("Should archive warehouse successfully when warehouse exists and not archived")
  void shouldArchiveWarehouseSuccessfully() {
    String businessUnitCode = "MWH.ARCHIVE." + System.currentTimeMillis();
    
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(45);
    warehouse.setStock(10);
    createWarehouseUseCase.create(warehouse);

    archiveWarehouseUseCase.archive(warehouse);

    Warehouse archived = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(archived, "Then warehouse should be found");
    assertNotNull(archived.getArchivedAt(), "Then archived timestamp should be set");
  }

  @Test
  @DisplayName("Should throw WarehouseNotFoundException when warehouse does not exist or has invalid business unit code")
  void shouldThrowWarehouseNotFoundException() {
    Warehouse warehouse1 = new Warehouse();
    warehouse1.setBusinessUnitCode("MWH.NONEXISTENT");
    warehouse1.setLocation("ZWOLLE-001");
    warehouse1.setCapacity(30);
    warehouse1.setStock(10);
    assertThrows(WarehouseNotFoundException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse1);
    }, "Then exception should be thrown when warehouse does not exist");

    Warehouse warehouse2 = new Warehouse();
    warehouse2.setBusinessUnitCode(null);
    warehouse2.setLocation("ZWOLLE-001");
    warehouse2.setCapacity(30);
    warehouse2.setStock(10);
    assertThrows(WarehouseNotFoundException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse2);
    }, "Then exception should be thrown when business unit code is null");

    Warehouse warehouse3 = new Warehouse();
    warehouse3.setBusinessUnitCode("");
    warehouse3.setLocation("ZWOLLE-001");
    warehouse3.setCapacity(30);
    warehouse3.setStock(10);
    assertThrows(WarehouseNotFoundException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse3);
    }, "Then exception should be thrown when business unit code is empty");
  }

  @Test
  @DisplayName("Should throw WarehouseAlreadyArchivedException when warehouse is already archived")
  void shouldThrowWarehouseAlreadyArchivedException() {
    String businessUnitCode = "MWH.ARCHIVE." + System.currentTimeMillis();
    
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation("ZWOLLE-002");
    warehouse.setCapacity(45);
    warehouse.setStock(10);
    createWarehouseUseCase.create(warehouse);

    archiveWarehouseUseCase.archive(warehouse);

    assertThrows(WarehouseAlreadyArchivedException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    }, "Then exception should be thrown when warehouse is already archived");
  }
}
