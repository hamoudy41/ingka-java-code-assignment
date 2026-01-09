package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

/**
 * Use case for creating new warehouses.
 * Validates business rules including business unit code uniqueness,
 * location validity, warehouse count limits, and capacity constraints.
 */
@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidationHelper validationHelper;

  /**
   * Creates a new warehouse after performing all necessary validations.
   * Validates: business unit code uniqueness, location existence,
   * warehouse count limits, and capacity constraints.
   *
   * @param warehouse the warehouse to create
   * @throws DuplicateBusinessUnitCodeException if business unit code already exists
   * @throws LocationNotFoundException if location is invalid
   * @throws LocationWarehouseLimitExceededException if location warehouse limit is reached
   * @throws LocationCapacityExceededException if capacity exceeds location maximum
   * @throws InsufficientCapacityException if stock exceeds capacity
   */
  @Override
  public void create(Warehouse warehouse) {
    log.debugf("Creating warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());

    validateBusinessUnitCodeUniqueness(warehouse.getBusinessUnitCode());
    
    var location = locationResolver.resolveByIdentifier(warehouse.getLocation());
    
    validationHelper.validateWarehouseCountLimit(warehouse.getLocation(), location.maxNumberOfWarehouses());
    validationHelper.validateCapacityConstraints(warehouse, location.maxCapacity());

    warehouse.setCreationAt(ZonedDateTime.now());
    warehouseStore.create(warehouse);
    
    log.infof("Successfully created warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
  }

  /**
   * Validates that the business unit code is unique.
   *
   * @param businessUnitCode the business unit code to validate
   * @throws DuplicateBusinessUnitCodeException if code already exists
   */
  private void validateBusinessUnitCodeUniqueness(String businessUnitCode) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    if (existing != null) {
      log.warnf("Attempted to create warehouse with duplicate business unit code '%s'", businessUnitCode);
      throw new DuplicateBusinessUnitCodeException(businessUnitCode);
    }
  }
}
