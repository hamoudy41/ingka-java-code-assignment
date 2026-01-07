package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Override
  public void archive(Warehouse warehouse) {
    log.debugf("Archiving warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.getBusinessUnitCode());
    if (existing == null) {
      log.warnf("Warehouse not found for archiving: '%s'", warehouse.getBusinessUnitCode());
      throw new WarehouseNotFoundException(warehouse.getBusinessUnitCode());
    }

    if (existing.getArchivedAt() != null) {
      log.warnf("Warehouse already archived: '%s'", warehouse.getBusinessUnitCode());
      throw new WarehouseAlreadyArchivedException(warehouse.getBusinessUnitCode());
    }

    existing.setArchivedAt(ZonedDateTime.now());
    warehouseStore.update(existing);
    
    log.infof("Successfully archived warehouse with business unit code '%s'", warehouse.getBusinessUnitCode());
  }
}
