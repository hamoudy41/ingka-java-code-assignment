package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

@Getter
public class InsufficientCapacityException extends WarehouseDomainException {

  private final int capacity;
  private final int requiredStock;

  public InsufficientCapacityException(int capacity, int requiredStock) {
    super(String.format("Warehouse capacity %d is insufficient to accommodate stock %d.",
        capacity, requiredStock));
    this.capacity = capacity;
    this.requiredStock = requiredStock;
  }
}

