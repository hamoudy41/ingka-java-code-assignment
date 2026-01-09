package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Port interface for warehouse persistence operations.
 * Defines the contract for storing and retrieving warehouse domain entities.
 */
public interface WarehouseStore {
  
  /**
   * Persists a new warehouse.
   *
   * @param warehouse the warehouse to create
   */
  void create(Warehouse warehouse);

  /**
   * Updates an existing warehouse.
   *
   * @param warehouse the warehouse with updated values
   */
  void update(Warehouse warehouse);

  /**
   * Removes a warehouse from storage.
   *
   * @param warehouse the warehouse to remove
   */
  void remove(Warehouse warehouse);

  /**
   * Finds a warehouse by its business unit code.
   *
   * @param buCode the business unit code
   * @return the warehouse, or null if not found
   */
  Warehouse findByBusinessUnitCode(String buCode);
}
