package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transaction;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.UUID;
import lombok.extern.jbosslog.JBossLog;

/**
 * Service for synchronizing store operations with legacy systems.
 * Registers transaction synchronization callbacks to ensure legacy sync happens after commit.
 */
@ApplicationScoped
@JBossLog
public class StoreSyncService {

  @Inject TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  @Inject TransactionManager transactionManager;

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject EntityManager entityManager;

  private static final String OPERATION_CREATE = "create";
  private static final String OPERATION_UPDATE = "update";

  public void scheduleCreateSync(Store store) {
    scheduleSync(store, OPERATION_CREATE, (reloadedStore, correlationId) -> {
      legacyStoreManagerGateway.createStoreOnLegacySystem(reloadedStore);
      log.infof("[%s] Synchronized store creation: %s (id: %d)",
          correlationId, reloadedStore.getName(), reloadedStore.getId());
    });
  }

  public void scheduleUpdateSync(Store store) {
    scheduleSync(store, OPERATION_UPDATE, (reloadedStore, correlationId) -> {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(reloadedStore);
      log.infof("[%s] Synchronized store update: %s (id: %d)",
          correlationId, reloadedStore.getName(), reloadedStore.getId());
    });
  }

  @FunctionalInterface
  private interface SyncAction {
    void execute(Store store, String correlationId) throws Exception;
  }

  private void scheduleSync(Store store, String operation, SyncAction syncAction) {
    if (!isValidForSync(store)) {
      return;
    }

    final StoreSnapshot snapshot = StoreSnapshot.from(store);
    final String correlationId = UUID.randomUUID().toString();

    registerPostCommitAction(correlationId, () -> {
      Store reloadedStore = reloadStore(snapshot.id());
      if (reloadedStore == null) {
        log.errorf("[%s] Store %d not found after commit, skipping %s sync",
            correlationId, snapshot.id(), operation);
        return;
      }

      logVersionMismatchIfAny(snapshot, reloadedStore, correlationId);
      executeSyncAction(syncAction, reloadedStore, correlationId, operation, snapshot.id());
    });
  }

  private boolean isValidForSync(Store store) {
    return store != null && store.getId() != null;
  }

  private void logVersionMismatchIfAny(StoreSnapshot snapshot, Store reloadedStore,
      String correlationId) {
    if (snapshot.version() != null && reloadedStore.getVersion() != null
        && !snapshot.version().equals(reloadedStore.getVersion())) {
      log.warnf("[%s] Version mismatch (expected: %d, actual: %d)",
          correlationId, snapshot.version(), reloadedStore.getVersion());
    }
  }

  private void executeSyncAction(SyncAction syncAction, Store reloadedStore, String correlationId,
      String operation, Long storeId) {
    try {
      syncAction.execute(reloadedStore, correlationId);
    } catch (Exception e) {
      log.errorf(e, "[%s] Legacy sync failed for %s (store: %d)",
          correlationId, operation, storeId);
    }
  }

  private Store reloadStore(Long storeId) {
    try {
      entityManager.clear();
      return entityManager.find(Store.class, storeId);
    } catch (Exception e) {
      log.errorf(e, "Failed to reload store with id: %d", storeId);
      return null;
    }
  }

  private void registerPostCommitAction(String correlationId, Runnable action) {
    if (action == null) {
      return;
    }

    if (!canRegisterSynchronization(correlationId)) {
      executeSafely(correlationId, action);
      return;
    }

    try {
      transactionSynchronizationRegistry.registerInterposedSynchronization(
          createSynchronization(correlationId, action));
    } catch (Exception e) {
      log.warnf(e, "[%s] Failed to register synchronization", correlationId);
      executeSafely(correlationId, action);
    }
  }

  private boolean canRegisterSynchronization(String correlationId) {
    if (transactionSynchronizationRegistry == null || transactionManager == null) {
      return false;
    }

    Transaction transaction = getCurrentTransaction(correlationId);
    return transaction != null && isValidTransactionState(transaction, correlationId);
  }

  private Transaction getCurrentTransaction(String correlationId) {
    try {
      return transactionManager.getTransaction();
    } catch (SystemException e) {
      log.errorf(e, "[%s] Failed to get current transaction", correlationId);
      return null;
    }
  }

  private boolean isValidTransactionState(Transaction transaction, String correlationId) {
    try {
      int status = transaction.getStatus();
      boolean valid = status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
      if (!valid) {
        log.warnf("[%s] Invalid transaction state: %d", correlationId, status);
      }
      return valid;
    } catch (SystemException e) {
      log.errorf(e, "[%s] Failed to check transaction status", correlationId);
      return false;
    }
  }

  private Synchronization createSynchronization(String correlationId, Runnable action) {
    return new Synchronization() {
      @Override
      public void beforeCompletion() {
      }

      @Override
      public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
          executeSafely(correlationId, action);
        }
      }
    };
  }

  private void executeSafely(String correlationId, Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      log.errorf(e, "[%s] Error executing action", correlationId);
    }
  }
}

