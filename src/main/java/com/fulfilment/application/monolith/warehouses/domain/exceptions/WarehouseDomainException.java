package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for warehouse-domain exceptions.
 *
 * <p>This lets JAX-RS exception mapping stay module-specific without intercepting exceptions from
 * other bounded contexts (e.g. stores).</p>
 */
public abstract class WarehouseDomainException extends RuntimeException {

  protected WarehouseDomainException(String message) {
    super(message);
  }

  protected WarehouseDomainException(String message, Throwable cause) {
    super(message, cause);
  }
}


