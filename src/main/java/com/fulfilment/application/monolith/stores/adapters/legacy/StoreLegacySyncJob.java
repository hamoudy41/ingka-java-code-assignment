package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.domain.events.StoreChangeType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Durable job record for legacy store synchronization.
 *
 * <p>This enables retries with backoff without introducing Kafka.</p>
 */
@Entity
@Table(
    name = "store_legacy_sync_job",
    indexes = {
        @Index(name = "idx_store_legacy_sync_job_due", columnList = "status,nextAttemptAt")
    }
)
public class StoreLegacySyncJob extends PanacheEntityBase {

  @Id
  public UUID id;

  @Column(nullable = false)
  public Long storeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public StoreChangeType type;

  public Long expectedVersion;

  @Column(nullable = false)
  public String correlationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public LegacySyncJobStatus status;

  @Column(nullable = false)
  public int attemptCount;

  @Column(nullable = false)
  public Instant nextAttemptAt;

  @Column(nullable = false)
  public Instant createdAt;

  @Column(nullable = false)
  public Instant updatedAt;

  @Column(length = 2000)
  public String lastError;

  @Version
  public Long version;

  public static StoreLegacySyncJob create(Long storeId, StoreChangeType type, Long expectedVersion,
      String correlationId) {
    StoreLegacySyncJob job = new StoreLegacySyncJob();
    job.id = UUID.randomUUID();
    job.storeId = storeId;
    job.type = type;
    job.expectedVersion = expectedVersion;
    job.correlationId = correlationId;
    job.status = LegacySyncJobStatus.PENDING;
    job.attemptCount = 0;
    job.nextAttemptAt = Instant.now();
    job.createdAt = Instant.now();
    job.updatedAt = job.createdAt;
    return job;
  }
}

