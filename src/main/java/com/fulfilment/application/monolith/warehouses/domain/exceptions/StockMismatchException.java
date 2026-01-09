package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when replacement stock does not match existing stock.
 */

@Getter
public class StockMismatchException extends RuntimeException {

  private final int existingStock;
  private final int newStock;

  public StockMismatchException(int existingStock, int newStock) {
    super(String.format("Stock mismatch. Existing stock: %d, New stock: %d. Stock must match during replacement.",
        existingStock, newStock));
    this.existingStock = existingStock;
    this.newStock = newStock;
  }
}

