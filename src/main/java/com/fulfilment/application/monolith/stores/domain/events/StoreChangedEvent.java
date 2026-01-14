package com.fulfilment.application.monolith.stores.domain.events;

/**
 * Domain event emitted when a store has been created or updated.
 *
 * <p>This event is intended to be general-purpose and can be observed for different reactions
 * (legacy synchronization, auditing, notifications, etc.). Consumers should treat it as a signal
 * and reload any required state from the database.</p>
 */
public record StoreChangedEvent(
    Long storeId,
    StoreChangeType type,
    Long expectedVersion,
    String correlationId
) {}

