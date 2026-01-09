package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.jbosslog.JBossLog;

/**
 * Repository implementation for warehouse persistence operations.
 * Handles CRUD operations and conversion between domain and database entities.
 */
@ApplicationScoped
@JBossLog
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  /**
   * Creates a new warehouse in the database.
   * Sets the creation timestamp if not already set.
   *
   * @param warehouse the warehouse domain entity to persist
   */
  @Override
  public void create(Warehouse warehouse) {
    log.debugf("Creating warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
    DbWarehouse dbWarehouse = toDbEntity(warehouse);
    if (dbWarehouse.getCreatedAt() == null) {
      dbWarehouse.setCreatedAt(LocalDateTime.now());
      log.debugf("Set creation timestamp for warehouse '%s'", warehouse.getBusinessUnitCode());
    }
    persist(dbWarehouse);
    log.infof("Successfully persisted warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
  }

  /**
   * Updates an existing warehouse in the database.
   * Throws RuntimeException if warehouse is not found.
   *
   * @param warehouse the warehouse domain entity with updated values
   * @throws RuntimeException if warehouse with the business unit code is not found
   */
  @Override
  public void update(Warehouse warehouse) {
    log.debugf("Updating warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
    DbWarehouse existing = find("businessUnitCode", warehouse.getBusinessUnitCode())
        .firstResultOptional()
        .orElseThrow(() -> {
          log.errorf("Warehouse not found for update: '%s'", warehouse.getBusinessUnitCode());
          return new RuntimeException("Warehouse not found for update");
        });
    
    existing.setLocation(warehouse.getLocation());
    existing.setCapacity(warehouse.getCapacity());
    existing.setStock(warehouse.getStock());
    
    if (warehouse.getArchivedAt() != null) {
      existing.setArchivedAt(warehouse.getArchivedAt()
          .withZoneSameInstant(ZoneId.of("UTC"))
          .toLocalDateTime());
      log.debugf("Updated archived timestamp for warehouse '%s'", warehouse.getBusinessUnitCode());
    }
    
    persist(existing);
    log.infof("Successfully updated warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
  }

  /**
   * Removes a warehouse from the database.
   * Throws RuntimeException if warehouse is not found.
   *
   * @param warehouse the warehouse domain entity to remove
   * @throws RuntimeException if warehouse with the business unit code is not found
   */
  @Override
  public void remove(Warehouse warehouse) {
    log.debugf("Removing warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.getBusinessUnitCode())
        .firstResultOptional()
        .orElseThrow(() -> {
          log.errorf("Warehouse not found for removal: '%s'", warehouse.getBusinessUnitCode());
          return new RuntimeException("Warehouse not found for removal");
        });
    delete(dbWarehouse);
    log.infof("Successfully removed warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
  }

  /**
   * Finds a warehouse by its business unit code.
   *
   * @param buCode the business unit code to search for
   * @return the warehouse domain entity if found, null otherwise
   */
  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    log.debugf("Finding warehouse by business unit code '%s'", buCode);
    Warehouse result = find("businessUnitCode", buCode)
        .firstResultOptional()
        .map(this::toDomainEntity)
        .orElse(null);
    if (result == null) {
      log.debugf("Warehouse not found for business unit code '%s'", buCode);
    } else {
      log.debugf("Found warehouse with business unit code '%s'", buCode);
    }
    return result;
  }

  /**
   * Counts the number of active (non-archived) warehouses at a given location.
   *
   * @param locationIdentifier the location identifier to count warehouses for
   * @return the count of active warehouses at the location
   */
  public long countByLocation(String locationIdentifier) {
    log.debugf("Counting active warehouses for location '%s'", locationIdentifier);
    long count = count("location = ?1 AND archivedAt IS NULL", locationIdentifier);
    log.debugf("Found %d active warehouses for location '%s'", count, locationIdentifier);
    return count;
  }

  /**
   * Converts a domain warehouse entity to a database entity.
   * Handles timezone conversion for timestamps.
   *
   * @param warehouse the domain warehouse entity
   * @return the database warehouse entity
   */
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

  /**
   * Converts a database warehouse entity to a domain entity.
   * Handles timezone conversion for timestamps.
   *
   * @param dbWarehouse the database warehouse entity
   * @return the domain warehouse entity
   */
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
