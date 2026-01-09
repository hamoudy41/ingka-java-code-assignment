package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Port interface for archiving warehouses.
 * Defines the contract for warehouse archival use cases.
 */
public interface ArchiveWarehouseOperation {
  
  /**
   * Archives a warehouse by setting its archived timestamp.
   *
   * @param warehouse the warehouse to archive
   */
  void archive(Warehouse warehouse);
}
