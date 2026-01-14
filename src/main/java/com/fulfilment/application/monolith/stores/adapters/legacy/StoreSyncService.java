package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.events.StoreChangeType;
import com.fulfilment.application.monolith.stores.domain.events.StoreChangedEvent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.extern.jbosslog.JBossLog;

/**
 * Service for synchronizing store operations with legacy systems.
 *
 * <p>This service publishes a general-purpose {@link StoreChangedEvent} inside the store write
 * transaction. After a successful commit, it enqueues a durable {@link StoreLegacySyncJob} and
 * attempts it immediately. Failures are retried asynchronously by {@link StoreLegacySyncJobWorker}.</p>
 */
@ApplicationScoped
@JBossLog
public class StoreSyncService {

  @Inject StoreLegacySyncJobWorker jobWorker;

  @Inject Event<StoreChangedEvent> storeEvents;

  public void scheduleCreateSync(Store store) {
    publishStoreEvent(store, StoreChangeType.CREATED);
  }

  public void scheduleUpdateSync(Store store) {
    publishStoreEvent(store, StoreChangeType.UPDATED);
  }

  private void publishStoreEvent(Store store, StoreChangeType type) {
    if (!isValidForSync(store)) {
      return;
    }

    final StoreSnapshot snapshot = StoreSnapshot.from(store);
    final String correlationId = UUID.randomUUID().toString();

    storeEvents.fire(new StoreChangedEvent(snapshot.id(), type, snapshot.version(), correlationId));
  }

  private boolean isValidForSync(Store store) {
    return store != null && store.getId() != null;
  }

  /**
   * Observes {@link StoreChangedEvent} only after a successful commit and enqueues a durable job.
   *
   * <p>The job is attempted immediately (in a new transaction) and, if it fails, will be retried
   * by the background worker.</p>
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    if (event == null || event.storeId() == null) {
      return;
    }

    if (event.type() == null) {
      log.warnf("[%s] Unknown store event type: null (store: %d)",
          event.correlationId(), event.storeId());
      return;
    }

    StoreLegacySyncJob job = StoreLegacySyncJob.create(
        event.storeId(),
        event.type(),
        event.expectedVersion(),
        event.correlationId()
    );
    job.persist();

    jobWorker.processJob(job);
  }
}

