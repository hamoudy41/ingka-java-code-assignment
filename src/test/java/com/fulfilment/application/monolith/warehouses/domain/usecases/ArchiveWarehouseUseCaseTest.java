package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
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
  WarehouseStore warehouseStore;

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

    Warehouse archived = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    assertNotNull(archived);
    assertNotNull(archived.getArchivedAt());
  }

  @Test
  @DisplayName("Should throw WarehouseNotFoundException when warehouse does not exist")
  void shouldThrowWarehouseNotFoundException() {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode("MWH.NONEXISTENT");
    warehouse.setLocation("ZWOLLE-001");
    warehouse.setCapacity(30);
    warehouse.setStock(10);

    assertThrows(WarehouseNotFoundException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });
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
    });
  }
}
