package com.fulfilment.application.monolith.stores.adapters.legacy;

/**
 * Event emitted within the store write transaction and observed AFTER_SUCCESS (after commit)
 * to trigger legacy system synchronization.
 *
 * <p>We intentionally publish only identifiers + metadata (not the JPA entity) to ensure the
 * sync always reloads committed state from the database.</p>
 */
public record StoreSyncEvent(
    Long storeId,
    String operation,
    Long expectedVersion,
    String correlationId
) {}

