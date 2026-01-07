package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = toDbEntity(warehouse);
    if (dbWarehouse.getCreatedAt() == null) {
      dbWarehouse.setCreatedAt(LocalDateTime.now());
    }
    persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse existing = find("businessUnitCode", warehouse.getBusinessUnitCode())
        .firstResultOptional()
        .orElseThrow(() -> new RuntimeException("Warehouse not found for update"));
    
    existing.setLocation(warehouse.getLocation());
    existing.setCapacity(warehouse.getCapacity());
    existing.setStock(warehouse.getStock());
    
    if (warehouse.getArchivedAt() != null) {
      existing.setArchivedAt(warehouse.getArchivedAt()
          .withZoneSameInstant(ZoneId.of("UTC"))
          .toLocalDateTime());
    }
    
    persist(existing);
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.getBusinessUnitCode())
        .firstResultOptional()
        .orElseThrow(() -> new RuntimeException("Warehouse not found for removal"));
    delete(dbWarehouse);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode", buCode)
        .firstResultOptional()
        .map(this::toDomainEntity)
        .orElse(null);
  }

  public long countByLocation(String locationIdentifier) {
    return count("location = ?1 AND archivedAt IS NULL", locationIdentifier);
  }

  private DbWarehouse toDbEntity(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.setBusinessUnitCode(warehouse.getBusinessUnitCode());
    dbWarehouse.setLocation(warehouse.getLocation());
    dbWarehouse.setCapacity(warehouse.getCapacity());
    dbWarehouse.setStock(warehouse.getStock());
    
    if (warehouse.getCreationAt() != null) {
      dbWarehouse.setCreatedAt(warehouse.getCreationAt()
          .withZoneSameInstant(ZoneId.of("UTC"))
          .toLocalDateTime());
    }
    
    if (warehouse.getArchivedAt() != null) {
      dbWarehouse.setArchivedAt(warehouse.getArchivedAt()
          .withZoneSameInstant(ZoneId.of("UTC"))
          .toLocalDateTime());
    }
    
    return dbWarehouse;
  }

  private Warehouse toDomainEntity(DbWarehouse dbWarehouse) {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(dbWarehouse.getBusinessUnitCode());
    warehouse.setLocation(dbWarehouse.getLocation());
    warehouse.setCapacity(dbWarehouse.getCapacity());
    warehouse.setStock(dbWarehouse.getStock());
    
    if (dbWarehouse.getCreatedAt() != null) {
      warehouse.setCreationAt(dbWarehouse.getCreatedAt().atZone(ZoneId.of("UTC")));
    }
    
    if (dbWarehouse.getArchivedAt() != null) {
      warehouse.setArchivedAt(dbWarehouse.getArchivedAt().atZone(ZoneId.of("UTC")));
    }
    
    return warehouse;
  }
}
