package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.extern.jbosslog.JBossLog;

/**
 * Service for synchronizing store operations with legacy systems.
 * Registers transaction synchronization callbacks to ensure legacy sync happens after commit.
 */
@ApplicationScoped
@JBossLog
public class StoreSyncService {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject EntityManager entityManager;

  @Inject Event<StoreSyncEvent> storeSyncEvents;

  public void scheduleCreateSync(Store store) {
    scheduleSync(store, StoreSyncOperation.CREATE);
  }

  public void scheduleUpdateSync(Store store) {
    scheduleSync(store, StoreSyncOperation.UPDATE);
  }

  private void scheduleSync(Store store, StoreSyncOperation operation) {
    if (!isValidForSync(store)) {
      return;
    }

    final StoreSnapshot snapshot = StoreSnapshot.from(store);
    final String correlationId = UUID.randomUUID().toString();

    storeSyncEvents.fire(new StoreSyncEvent(snapshot.id(), operation, snapshot.version(), correlationId));
  }

  private boolean isValidForSync(Store store) {
    return store != null && store.getId() != null;
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void onStoreSync(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
    if (event == null || event.storeId() == null) {
      return;
    }

    Store reloadedStore = reloadStore(event.storeId());
    if (reloadedStore == null) {
      log.errorf("[%s] Store %d not found after commit, skipping %s sync",
          event.correlationId(), event.storeId(), event.operation());
      return;
    }

    logVersionMismatchIfAny(event, reloadedStore);

    if (event.operation() == null) {
      log.warnf("[%s] Unknown store sync operation: null (store: %d)",
          event.correlationId(), event.storeId());
      return;
    }

    switch (event.operation()) {
      case CREATE -> executeSafely(event.correlationId(), () -> {
        legacyStoreManagerGateway.createStoreOnLegacySystem(reloadedStore);
        log.infof("[%s] Synchronized store creation: %s (id: %d)",
            event.correlationId(), reloadedStore.getName(), reloadedStore.getId());
      });
      case UPDATE -> executeSafely(event.correlationId(), () -> {
        legacyStoreManagerGateway.updateStoreOnLegacySystem(reloadedStore);
        log.infof("[%s] Synchronized store update: %s (id: %d)",
            event.correlationId(), reloadedStore.getName(), reloadedStore.getId());
      });
    }
  }

  private void logVersionMismatchIfAny(StoreSyncEvent event, Store reloadedStore) {
    if (event.expectedVersion() != null && reloadedStore.getVersion() != null
        && !event.expectedVersion().equals(reloadedStore.getVersion())) {
      log.warnf("[%s] Version mismatch (expected: %d, actual: %d)",
          event.correlationId(), event.expectedVersion(), reloadedStore.getVersion());
    }
  }

  private Store reloadStore(Long storeId) {
    try {
      // Called from a REQUIRES_NEW transaction (AFTER_SUCCESS observer), so we already have a
      // fresh persistence context relative to the original write TX.
      return entityManager.find(Store.class, storeId);
    } catch (Exception e) {
      log.errorf(e, "Failed to reload store with id: %d", storeId);
      return null;
    }
  }

  private void executeSafely(String correlationId, Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      log.errorf(e, "[%s] Error executing action", correlationId);
    }
  }
}

