package com.fulfilment.application.monolith.stores.adapters.legacy;

/**
 * Event emitted within the store write transaction and observed AFTER_SUCCESS (after commit)
 * to trigger legacy system synchronization.
 *
 * <p>We intentionally publish only identifiers + metadata (not the JPA entity) and then reload the
 * store in a separate transaction, so the legacy sync uses committed state.</p>
 */
public record StoreSyncEvent(
    Long storeId,
    StoreSyncOperation operation,
    Long expectedVersion,
    String correlationId
) {}

