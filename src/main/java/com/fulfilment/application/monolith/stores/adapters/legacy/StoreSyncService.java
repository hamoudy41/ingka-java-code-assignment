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

  private static final String OPERATION_CREATE = "create";
  private static final String OPERATION_UPDATE = "update";

  public void scheduleCreateSync(Store store) {
    scheduleSync(store, OPERATION_CREATE);
  }

  public void scheduleUpdateSync(Store store) {
    scheduleSync(store, OPERATION_UPDATE);
  }

  private void scheduleSync(Store store, String operation) {
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

    if (OPERATION_CREATE.equals(event.operation())) {
      executeSafely(event.correlationId(), () -> {
        legacyStoreManagerGateway.createStoreOnLegacySystem(reloadedStore);
        log.infof("[%s] Synchronized store creation: %s (id: %d)",
            event.correlationId(), reloadedStore.getName(), reloadedStore.getId());
      });
      return;
    }

    if (OPERATION_UPDATE.equals(event.operation())) {
      executeSafely(event.correlationId(), () -> {
        legacyStoreManagerGateway.updateStoreOnLegacySystem(reloadedStore);
        log.infof("[%s] Synchronized store update: %s (id: %d)",
            event.correlationId(), reloadedStore.getName(), reloadedStore.getId());
      });
      return;
    }

    log.warnf("[%s] Unknown store sync operation: %s (store: %d)",
        event.correlationId(), event.operation(), event.storeId());
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

