package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.StockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidationHelper validationHelper;

  @Override
  public void replace(Warehouse newWarehouse) {
    log.debugf("Replacing warehouse with business unit code '%s'", newWarehouse.getBusinessUnitCode());

    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.getBusinessUnitCode());
    if (existing == null) {
      log.warnf("Warehouse not found for replacement: '%s'", newWarehouse.getBusinessUnitCode());
      throw new WarehouseNotFoundException(newWarehouse.getBusinessUnitCode());
    }

    validateNewBusinessUnitCodeUniqueness(newWarehouse.getBusinessUnitCode(), existing.getBusinessUnitCode());
    
    var location = locationResolver.resolveByIdentifier(newWarehouse.getLocation());
    
    if (!newWarehouse.getLocation().equals(existing.getLocation())) {
      validationHelper.validateWarehouseCountLimit(newWarehouse.getLocation(), location.maxNumberOfWarehouses());
    }
    
    validationHelper.validateCapacityAgainstLocationMax(newWarehouse, location.maxCapacity());
    validateReplacementConstraints(newWarehouse, existing);

    warehouseStore.update(newWarehouse);
    
    log.infof("Successfully replaced warehouse with business unit code '%s'", newWarehouse.getBusinessUnitCode());
  }

  private void validateNewBusinessUnitCodeUniqueness(String newBusinessUnitCode, String existingBusinessUnitCode) {
    if (!newBusinessUnitCode.equals(existingBusinessUnitCode)) {
      Warehouse duplicate = warehouseStore.findByBusinessUnitCode(newBusinessUnitCode);
      if (duplicate != null) {
        log.warnf("Attempted to replace warehouse with duplicate business unit code '%s'", newBusinessUnitCode);
        throw new DuplicateBusinessUnitCodeException(newBusinessUnitCode);
      }
    }
  }

  private void validateReplacementConstraints(Warehouse newWarehouse, Warehouse existing) {
    if (newWarehouse.getCapacity() < existing.getStock()) {
      log.warnf("New capacity %d is insufficient for existing stock %d",
          newWarehouse.getCapacity(), existing.getStock());
      throw new InsufficientCapacityException(newWarehouse.getCapacity(), existing.getStock());
    }

    if (!newWarehouse.getStock().equals(existing.getStock())) {
      log.warnf("Stock mismatch. Existing: %d, New: %d", existing.getStock(), newWarehouse.getStock());
      throw new StockMismatchException(existing.getStock(), newWarehouse.getStock());
    }
  }
}
