package com.fulfilment.application.monolith.stores.adapters.legacy;

/**
 * Persistence-backed status for legacy synchronization jobs.
 */
public enum LegacySyncJobStatus {
  PENDING,
  RETRY,
  SUCCEEDED,
  FAILED
}

