package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.exceptions.InvalidStoreSnapshotException;

/**
 * Immutable snapshot of a store's state for legacy system synchronization.
 * Captures the essential store data at a point in time.
 */
public record StoreSnapshot(
    Long id,
    String name,
    int quantityProductsInStock,
    Long version
) {
  public StoreSnapshot {
    if (id == null) {
      throw new InvalidStoreSnapshotException("Store id cannot be null");
    }
    if (name == null) {
      throw new InvalidStoreSnapshotException("Store name cannot be null");
    }
  }

  public static StoreSnapshot from(Store store) {
    if (store == null) {
      throw new InvalidStoreSnapshotException("Store cannot be null");
    }
    if (store.getId() == null) {
      throw new InvalidStoreSnapshotException("Store id cannot be null");
    }
    return new StoreSnapshot(store.getId(), store.getName(), store.getQuantityProductsInStock(), store.getVersion());
  }
}

