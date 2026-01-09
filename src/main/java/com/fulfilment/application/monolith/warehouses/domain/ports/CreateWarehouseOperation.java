package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Port interface for creating warehouses.
 * Defines the contract for warehouse creation use cases.
 */
public interface CreateWarehouseOperation {
  
  /**
   * Creates a new warehouse with validation.
   *
   * @param warehouse the warehouse to create
   */
  void create(Warehouse warehouse);
}
