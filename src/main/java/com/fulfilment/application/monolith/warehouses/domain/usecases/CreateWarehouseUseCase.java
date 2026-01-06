package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Override
  public void create(Warehouse warehouse) {
    // TODO implement this method

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
