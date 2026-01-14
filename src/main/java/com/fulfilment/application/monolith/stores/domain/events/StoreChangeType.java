package com.fulfilment.application.monolith.stores.domain.events;

/**
 * High-level store lifecycle changes emitted by the stores bounded context.
 *
 * <p>These events can be consumed by multiple concerns (legacy integration, auditing, etc.).</p>
 */
public enum StoreChangeType {
  CREATED,
  UPDATED
}

