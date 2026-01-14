package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.events.StoreChangeType;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.jbosslog.JBossLog;

/**
 * Background worker that processes {@link StoreLegacySyncJob} records.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Pick up due jobs (PENDING/RETRY) and execute the legacy operation</li>
 *   <li>Record success/failure and schedule retries using exponential backoff</li>
 *   <li>Cap retries to a small maximum to avoid infinite loops</li>
 * </ul>
 */
@ApplicationScoped
@JBossLog
public class StoreLegacySyncJobWorker {

  private static final int MAX_ATTEMPTS = 5;
  private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);
  private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);
  private static final int BATCH_SIZE = 25;

  @Inject EntityManager entityManager;
  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  /**
   * Processes due jobs using pessimistic locking to prevent concurrent processing.
   *
   * <p>Uses SELECT FOR UPDATE to ensure only one thread processes a job at a time.</p>
   */
  @Transactional
  public void processDueJobs() {
    Instant now = Instant.now();
    List<StoreLegacySyncJob> jobs = entityManager
        .createQuery(
            "SELECT j FROM StoreLegacySyncJob j WHERE j.status IN :statuses AND j.nextAttemptAt <= :now ORDER BY j.createdAt",
            StoreLegacySyncJob.class)
        .setParameter("statuses", List.of(LegacySyncJobStatus.PENDING, LegacySyncJobStatus.RETRY))
        .setParameter("now", now)
        .setMaxResults(BATCH_SIZE)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultList();

    for (StoreLegacySyncJob job : jobs) {
      processJob(job);
    }
  }

  /**
   * Processes a single job, handling optimistic locking conflicts gracefully.
   *
   * <p>If another thread has already processed this job (OptimisticLockException), this call
   * is a no-op.</p>
   */
  @Transactional
  public void processJob(StoreLegacySyncJob job) {
    if (job == null || job.id == null) {
      return;
    }

    // Reload with lock to ensure we have the latest state and prevent concurrent processing.
    StoreLegacySyncJob lockedJob = entityManager.find(StoreLegacySyncJob.class, job.id, LockModeType.PESSIMISTIC_WRITE);
    if (lockedJob == null) {
      log.warnf("Job %s no longer exists", job.id);
      return;
    }

    if (lockedJob.status == LegacySyncJobStatus.SUCCEEDED || lockedJob.status == LegacySyncJobStatus.FAILED) {
      return;
    }

    try {
      attempt(lockedJob);
    } catch (OptimisticLockException ex) {
      log.debugf(ex, "[%s] Job %s was concurrently modified, skipping", lockedJob.correlationId, lockedJob.id);
    }
  }

  private void attempt(StoreLegacySyncJob job) {
    job.attemptCount += 1;
    job.updatedAt = Instant.now();

    Store store = entityManager.find(Store.class, job.storeId);
    if (store == null) {
      fail(job, "Store not found: " + job.storeId);
      return;
    }

    try {
      if (job.type == StoreChangeType.CREATED) {
        legacyStoreManagerGateway.createStoreOnLegacySystem(store);
      } else if (job.type == StoreChangeType.UPDATED) {
        legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
      } else {
        fail(job, "Unknown store change type: " + job.type);
        return;
      }

      job.status = LegacySyncJobStatus.SUCCEEDED;
      job.lastError = null;
      job.nextAttemptAt = Instant.EPOCH;
      job.updatedAt = Instant.now();
      log.infof("[%s] Legacy sync succeeded for store %d (%s) after %d attempt(s)",
          job.correlationId, job.storeId, job.type, job.attemptCount);
    } catch (LegacySyncException ex) {
      retryOrFail(job, ex);
    } catch (Exception ex) {
      retryOrFail(job, ex);
    }
  }

  private void retryOrFail(StoreLegacySyncJob job, Exception ex) {
    String msg = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
    job.lastError = msg;

    if (job.attemptCount >= MAX_ATTEMPTS) {
      fail(job, msg);
      log.errorf(ex, "[%s] Legacy sync permanently failed for store %d (%s) after %d attempts",
          job.correlationId, job.storeId, job.type, job.attemptCount);
      return;
    }

    Duration backoff = computeBackoff(job.attemptCount);
    job.status = LegacySyncJobStatus.RETRY;
    job.nextAttemptAt = Instant.now().plus(backoff);
    job.updatedAt = Instant.now();
    log.warnf(ex, "[%s] Legacy sync failed for store %d (%s), retrying in %dms (attempt %d/%d)",
        job.correlationId, job.storeId, job.type, backoff.toMillis(), job.attemptCount, MAX_ATTEMPTS);
  }

  private void fail(StoreLegacySyncJob job, String error) {
    job.status = LegacySyncJobStatus.FAILED;
    job.lastError = error;
    job.nextAttemptAt = Instant.EPOCH;
    job.updatedAt = Instant.now();
  }

  /**
   * Computes an exponential backoff delay based on the current attempt number.
   *
   * <p>The first failure schedules a retry after {@link #INITIAL_BACKOFF}, doubling each time,
   * capped at {@link #MAX_BACKOFF}.</p>
   */
  private Duration computeBackoff(int attemptCount) {
    long factor = 1L << Math.max(0, attemptCount - 1);
    long millis = INITIAL_BACKOFF.toMillis() * factor;
    return Duration.ofMillis(Math.min(millis, MAX_BACKOFF.toMillis()));
  }
}

