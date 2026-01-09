package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Port interface for replacing warehouses.
 * Defines the contract for warehouse replacement use cases.
 */
public interface ReplaceWarehouseOperation {
  
  /**
   * Replaces an existing warehouse with new values.
   *
   * @param warehouse the warehouse with updated values
   */
  void replace(Warehouse warehouse);
}
