package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class WarehouseValidationHelper {

  @Inject
  WarehouseRepository warehouseRepository;

  public void validateWarehouseCountLimit(String locationIdentifier, int maxWarehouses) {
    long currentCount = warehouseRepository.countByLocation(locationIdentifier);
    if (currentCount >= maxWarehouses) {
      log.warnf("Warehouse limit exceeded for location '%s'. Current: %d, Max: %d",
          locationIdentifier, currentCount, maxWarehouses);
      throw new LocationWarehouseLimitExceededException(locationIdentifier, (int) currentCount, maxWarehouses);
    }
  }

  public void validateCapacityAgainstLocationMax(Warehouse warehouse, int maxCapacity) {
    if (warehouse.getCapacity() > maxCapacity) {
      log.warnf("Capacity %d exceeds maximum %d for location '%s'",
          warehouse.getCapacity(), maxCapacity, warehouse.getLocation());
      throw new LocationCapacityExceededException(warehouse.getLocation(), warehouse.getCapacity(), maxCapacity);
    }
  }

  public void validateStockAgainstCapacity(Warehouse warehouse) {
    if (warehouse.getStock() > warehouse.getCapacity()) {
      log.warnf("Stock %d exceeds capacity %d for warehouse '%s'",
          warehouse.getStock(), warehouse.getCapacity(), warehouse.getBusinessUnitCode());
      throw new InsufficientCapacityException(warehouse.getCapacity(), warehouse.getStock());
    }
  }

  public void validateCapacityConstraints(Warehouse warehouse, int maxCapacity) {
    validateCapacityAgainstLocationMax(warehouse, maxCapacity);
    validateStockAgainstCapacity(warehouse);
  }
}

