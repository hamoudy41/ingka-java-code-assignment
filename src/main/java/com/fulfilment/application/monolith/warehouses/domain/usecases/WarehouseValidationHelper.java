package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

/**
 * Helper class for warehouse validation operations.
 * Provides reusable validation methods for warehouse count limits,
 * capacity constraints, and stock validation.
 */
@ApplicationScoped
@JBossLog
public class WarehouseValidationHelper {

  @Inject
  WarehouseRepository warehouseRepository;

  /**
   * Validates that the warehouse count for a location does not exceed the maximum.
   *
   * @param locationIdentifier the location identifier
   * @param maxWarehouses the maximum number of warehouses allowed at this location
   * @throws LocationWarehouseLimitExceededException if limit is exceeded
   */
  public void validateWarehouseCountLimit(String locationIdentifier, int maxWarehouses) {
    long currentCount = warehouseRepository.countByLocation(locationIdentifier);
    if (currentCount >= maxWarehouses) {
      log.warnf("Warehouse limit exceeded for location '%s'. Current: %d, Max: %d",
          locationIdentifier, currentCount, maxWarehouses);
      throw new LocationWarehouseLimitExceededException(locationIdentifier, (int) currentCount, maxWarehouses);
    }
  }

  /**
   * Validates that warehouse capacity does not exceed location maximum.
   *
   * @param warehouse the warehouse to validate
   * @param maxCapacity the maximum capacity allowed for the location
   * @throws LocationCapacityExceededException if capacity exceeds maximum
   */
  public void validateCapacityAgainstLocationMax(Warehouse warehouse, int maxCapacity) {
    if (warehouse.getCapacity() > maxCapacity) {
      log.warnf("Capacity %d exceeds maximum %d for location '%s'",
          warehouse.getCapacity(), maxCapacity, warehouse.getLocation());
      throw new LocationCapacityExceededException(warehouse.getLocation(), warehouse.getCapacity(), maxCapacity);
    }
  }

  /**
   * Validates that warehouse stock does not exceed capacity.
   *
   * @param warehouse the warehouse to validate
   * @throws InsufficientCapacityException if stock exceeds capacity
   */
  public void validateStockAgainstCapacity(Warehouse warehouse) {
    if (warehouse.getStock() > warehouse.getCapacity()) {
      log.warnf("Stock %d exceeds capacity %d for warehouse '%s'",
          warehouse.getStock(), warehouse.getCapacity(), warehouse.getBusinessUnitCode());
      throw new InsufficientCapacityException(warehouse.getCapacity(), warehouse.getStock());
    }
  }

  /**
   * Validates both capacity against location maximum and stock against capacity.
   *
   * @param warehouse the warehouse to validate
   * @param maxCapacity the maximum capacity allowed for the location
   * @throws LocationCapacityExceededException if capacity exceeds maximum
   * @throws InsufficientCapacityException if stock exceeds capacity
   */
  public void validateCapacityConstraints(Warehouse warehouse, int maxCapacity) {
    validateCapacityAgainstLocationMax(warehouse, maxCapacity);
    validateStockAgainstCapacity(warehouse);
  }
}

