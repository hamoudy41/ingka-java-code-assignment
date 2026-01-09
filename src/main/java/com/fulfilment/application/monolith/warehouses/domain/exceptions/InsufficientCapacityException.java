package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when warehouse stock exceeds capacity.
 */
@Getter
public class InsufficientCapacityException extends RuntimeException {

  private final int capacity;
  private final int requiredStock;

  public InsufficientCapacityException(int capacity, int requiredStock) {
    super(String.format("Warehouse capacity %d is insufficient to accommodate stock %d.",
        capacity, requiredStock));
    this.capacity = capacity;
    this.requiredStock = requiredStock;
  }
}

