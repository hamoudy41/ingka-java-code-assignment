package com.fulfilment.application.monolith.stores.adapters.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.events.StoreChangeType;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StoreLegacySyncJobWorkerTest {

  @Inject StoreLegacySyncJobWorker worker;
  @Inject EntityManager entityManager;

  private final AtomicInteger createCalls = new AtomicInteger();
  private final AtomicInteger updateCalls = new AtomicInteger();
  private volatile boolean shouldFail = false;
  private volatile RuntimeException failureException = null;

  @BeforeEach
  @Transactional
  void cleanup() {
    StoreLegacySyncJob.deleteAll();
    Store.delete("name LIKE ?1", "Test Store%");
    createCalls.set(0);
    updateCalls.set(0);
    shouldFail = false;
    failureException = null;

    QuarkusMock.installMockForType(new LegacyStoreManagerGateway() {
      @Override
      public void createStoreOnLegacySystem(Store store) {
        createCalls.incrementAndGet();
        if (shouldFail) {
          if (failureException != null) {
            throw failureException;
          }
          throw new LegacySyncException("Simulated legacy sync failure");
        }
      }

      @Override
      public void updateStoreOnLegacySystem(Store store) {
        updateCalls.incrementAndGet();
        if (shouldFail) {
          if (failureException != null) {
            throw failureException;
          }
          throw new LegacySyncException("Simulated legacy sync failure");
        }
      }
    }, LegacyStoreManagerGateway.class);
  }

  @Test
  @DisplayName("Should successfully process a PENDING job")
  @Transactional
  void shouldSuccessfullyProcessPendingJob() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");

    worker.processJob(job);

    StoreLegacySyncJob updated = entityManager.find(StoreLegacySyncJob.class, job.id);
    assertNotNull(updated, "Job should exist after processing");
    assertEquals(LegacySyncJobStatus.SUCCEEDED, updated.status, "Job status should be SUCCEEDED");
    assertEquals(1, updated.attemptCount, "Attempt count should be 1");
    assertNull(updated.lastError, "Last error should be null on success");
    assertEquals(1, createCalls.get(), "Legacy gateway create should be called once");
  }

  @Test
  @DisplayName("Should retry job on LegacySyncException with exponential backoff")
  @Transactional
  void shouldRetryJobOnLegacySyncException() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    shouldFail = true;

    worker.processJob(job);

    StoreLegacySyncJob updated = entityManager.find(StoreLegacySyncJob.class, job.id);
    assertNotNull(updated, "Job should exist after processing");
    assertEquals(LegacySyncJobStatus.RETRY, updated.status, "Job status should be RETRY on failure");
    assertEquals(1, updated.attemptCount, "Attempt count should be 1");
    assertNotNull(updated.lastError, "Last error should be set on failure");
    assertTrue(updated.nextAttemptAt.isAfter(Instant.now()), "Next attempt time should be in the future");
    assertTrue(updated.nextAttemptAt.isBefore(Instant.now().plus(Duration.ofSeconds(1))), "Next attempt time should be within 1 second");
    assertEquals(1, createCalls.get(), "Legacy gateway create should be called once");
  }

  @Test
  @DisplayName("Should retry job on generic Exception with exponential backoff")
  @Transactional
  void shouldRetryJobOnGenericException() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.UPDATED, "test-correlation");
    shouldFail = true;
    failureException = new RuntimeException("Generic failure");

    worker.processJob(job);

    StoreLegacySyncJob updated = entityManager.find(StoreLegacySyncJob.class, job.id);
    assertNotNull(updated, "Job should exist after processing");
    assertEquals(LegacySyncJobStatus.RETRY, updated.status, "Job status should be RETRY on failure");
    assertEquals(1, updated.attemptCount, "Attempt count should be 1");
    assertNotNull(updated.lastError, "Last error should be set on failure");
    assertTrue(updated.lastError.contains("Generic failure"), "Last error should contain exception message");
    assertEquals(1, updateCalls.get(), "Legacy gateway update should be called once");
  }

  @Test
  @DisplayName("Should apply exponential backoff: attempt 1 = 200ms, attempt 2 = 400ms, attempt 3 = 800ms")
  @Transactional
  void shouldApplyExponentialBackoff() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    shouldFail = true;
    entityManager.flush();

    worker.processJob(job);
    entityManager.flush();
    Instant afterProcessing = Instant.now();
    StoreLegacySyncJob afterFirst = reloadJob(job.id);
    long firstBackoff = Duration.between(afterProcessing, afterFirst.nextAttemptAt).toMillis();
    assertTrue(firstBackoff >= 150 && firstBackoff <= 300, "First backoff should be ~200ms, got: " + firstBackoff);

    afterFirst.attemptCount = 1;
    afterFirst.status = LegacySyncJobStatus.RETRY;
    afterFirst.nextAttemptAt = Instant.now();
    entityManager.merge(afterFirst);
    entityManager.flush();

    worker.processJob(afterFirst);
    entityManager.flush();
    Instant afterSecondProcessing = Instant.now();
    StoreLegacySyncJob afterSecond = reloadJob(job.id);
    long secondBackoff = Duration.between(afterSecondProcessing, afterSecond.nextAttemptAt).toMillis();
    assertTrue(secondBackoff >= 350 && secondBackoff <= 500, "Second backoff should be ~400ms, got: " + secondBackoff);
    assertTrue(secondBackoff > firstBackoff, "Second backoff should be greater than first");

    afterSecond.attemptCount = 2;
    afterSecond.status = LegacySyncJobStatus.RETRY;
    afterSecond.nextAttemptAt = Instant.now();
    entityManager.merge(afterSecond);
    entityManager.flush();

    worker.processJob(afterSecond);
    entityManager.flush();
    Instant afterThirdProcessing = Instant.now();
    StoreLegacySyncJob afterThird = reloadJob(job.id);
    long thirdBackoff = Duration.between(afterThirdProcessing, afterThird.nextAttemptAt).toMillis();
    assertTrue(thirdBackoff >= 750 && thirdBackoff <= 900, "Third backoff should be ~800ms, got: " + thirdBackoff);
    assertTrue(thirdBackoff > secondBackoff, "Third backoff should be greater than second");
  }

  @Test
  @DisplayName("Should fail job after MAX_ATTEMPTS (5) retries")
  @Transactional
  void shouldFailJobAfterMaxAttempts() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    shouldFail = true;
    entityManager.flush();

    for (int i = 1; i <= 5; i++) {
      worker.processJob(job);
      entityManager.flush();
      job = reloadJob(job.id);
      if (i < 5) {
        assertEquals(LegacySyncJobStatus.RETRY, job.status, "Should be RETRY before max attempts");
        assertEquals(i, job.attemptCount, "Attempt count should match iteration");
        job.nextAttemptAt = Instant.now();
        entityManager.merge(job);
        entityManager.flush();
      }
    }

    StoreLegacySyncJob finalJob = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.FAILED, finalJob.status, "Job should be FAILED after max attempts");
    assertEquals(5, finalJob.attemptCount, "Attempt count should be 5");
    assertNotNull(finalJob.lastError, "Last error should be set on failure");
    assertEquals(5, createCalls.get(), "Legacy gateway create should be called 5 times");
  }

  @Test
  @DisplayName("Should succeed on retry after initial failure")
  @Transactional
  void shouldSucceedOnRetryAfterInitialFailure() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    shouldFail = true;
    entityManager.flush();

    worker.processJob(job);
    entityManager.flush();
    StoreLegacySyncJob retryJob = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.RETRY, retryJob.status, "Job should be RETRY after first failure");
    assertEquals(1, retryJob.attemptCount, "Attempt count should be 1 after first attempt");

    shouldFail = false;
    retryJob.nextAttemptAt = Instant.now();
    entityManager.merge(retryJob);
    entityManager.flush();

    worker.processJob(retryJob);
    entityManager.flush();
    StoreLegacySyncJob succeededJob = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.SUCCEEDED, succeededJob.status, "Job should be SUCCEEDED after retry");
    assertEquals(2, succeededJob.attemptCount, "Attempt count should be 2 after retry");
    assertEquals(2, createCalls.get(), "Legacy gateway create should be called twice");
  }

  @Test
  @DisplayName("Should fail job when store is not found")
  @Transactional
  void shouldFailJobWhenStoreNotFound() {
    StoreLegacySyncJob job = createJob(99999L, StoreChangeType.CREATED, "test-correlation");

    worker.processJob(job);

    StoreLegacySyncJob updated = entityManager.find(StoreLegacySyncJob.class, job.id);
    assertNotNull(updated, "Job should exist after processing");
    assertEquals(LegacySyncJobStatus.FAILED, updated.status, "Job should be FAILED when store not found");
    assertTrue(updated.lastError.contains("Store not found"), "Last error should indicate store not found");
    assertEquals(0, createCalls.get(), "Legacy gateway should not be called when store not found");
  }

  @Test
  @DisplayName("Should not process job that is already SUCCEEDED")
  @Transactional
  void shouldNotProcessAlreadySucceededJob() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    job.status = LegacySyncJobStatus.SUCCEEDED;
    entityManager.merge(job);
    entityManager.flush();

    worker.processJob(job);

    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for already succeeded job");
    StoreLegacySyncJob updated = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.SUCCEEDED, updated.status, "Job status should remain SUCCEEDED");
  }

  @Test
  @DisplayName("Should not process job that is already FAILED")
  @Transactional
  void shouldNotProcessAlreadyFailedJob() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    job.status = LegacySyncJobStatus.FAILED;
    entityManager.merge(job);
    entityManager.flush();

    worker.processJob(job);

    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for already failed job");
    StoreLegacySyncJob updated = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.FAILED, updated.status, "Job status should remain FAILED");
  }

  @Test
  @DisplayName("Should process multiple due jobs in batch")
  @Transactional
  void shouldProcessMultipleDueJobsInBatch() {
    Store store1 = createAndPersistStore("Test Store 1", 10);
    Store store2 = createAndPersistStore("Test Store 2", 20);
    Store store3 = createAndPersistStore("Test Store 3", 30);

    StoreLegacySyncJob job1 = createJob(store1.getId(), StoreChangeType.CREATED, "corr-1");
    StoreLegacySyncJob job2 = createJob(store2.getId(), StoreChangeType.CREATED, "corr-2");
    StoreLegacySyncJob job3 = createJob(store3.getId(), StoreChangeType.CREATED, "corr-3");
    entityManager.flush();

    worker.processDueJobs();
    entityManager.flush();

    assertEquals(LegacySyncJobStatus.SUCCEEDED, reloadJob(job1.id).status, "First job should be SUCCEEDED");
    assertEquals(LegacySyncJobStatus.SUCCEEDED, reloadJob(job2.id).status, "Second job should be SUCCEEDED");
    assertEquals(LegacySyncJobStatus.SUCCEEDED, reloadJob(job3.id).status, "Third job should be SUCCEEDED");
    assertEquals(3, createCalls.get(), "Legacy gateway create should be called 3 times");
  }

  @Test
  @DisplayName("Should skip jobs that are not yet due")
  @Transactional
  void shouldSkipJobsNotYetDue() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.CREATED, "test-correlation");
    job.nextAttemptAt = Instant.now().plus(Duration.ofHours(1));
    entityManager.merge(job);
    entityManager.flush();

    worker.processDueJobs();

    StoreLegacySyncJob updated = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.PENDING, updated.status, "Job should remain PENDING when not yet due");
    assertEquals(0, updated.attemptCount, "Attempt count should remain 0");
    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for job not yet due");
  }

  @Test
  @DisplayName("Should skip jobs that are already SUCCEEDED or FAILED")
  @Transactional
  void shouldSkipCompletedJobs() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob succeededJob = createJob(store.getId(), StoreChangeType.CREATED, "corr-1");
    succeededJob.status = LegacySyncJobStatus.SUCCEEDED;
    entityManager.merge(succeededJob);
    entityManager.flush();

    Store store2 = createAndPersistStore("Test Store 2", 20);
    StoreLegacySyncJob failedJob = createJob(store2.getId(), StoreChangeType.CREATED, "corr-2");
    failedJob.status = LegacySyncJobStatus.FAILED;
    entityManager.merge(failedJob);
    entityManager.flush();

    worker.processDueJobs();

    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for completed jobs");
  }

  @Test
  @DisplayName("Should handle null job gracefully")
  @Transactional
  void shouldHandleNullJobGracefully() {
    worker.processJob(null);
    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for null job");
  }

  @Test
  @DisplayName("Should handle job with null id gracefully")
  @Transactional
  void shouldHandleJobWithNullIdGracefully() {
    StoreLegacySyncJob job = new StoreLegacySyncJob();
    job.id = null;
    worker.processJob(job);
    assertEquals(0, createCalls.get(), "Legacy gateway should not be called for job with null id");
  }

  @Test
  @DisplayName("Should process UPDATE type jobs")
  @Transactional
  void shouldProcessUpdateTypeJobs() {
    Store store = createAndPersistStore("Test Store", 10);
    StoreLegacySyncJob job = createJob(store.getId(), StoreChangeType.UPDATED, "test-correlation");
    entityManager.flush();

    worker.processJob(job);
    entityManager.flush();

    StoreLegacySyncJob updated = reloadJob(job.id);
    assertEquals(LegacySyncJobStatus.SUCCEEDED, updated.status, "UPDATE type job should be SUCCEEDED");
    assertEquals(1, updateCalls.get(), "Legacy gateway update should be called once");
    assertEquals(0, createCalls.get(), "Legacy gateway create should not be called");
  }

  private Store createAndPersistStore(String name, int stock) {
    Store store = new Store();
    store.setName(name + " " + System.currentTimeMillis());
    store.setQuantityProductsInStock(stock);
    entityManager.persist(store);
    entityManager.flush();
    return store;
  }

  private StoreLegacySyncJob createJob(Long storeId, StoreChangeType type, String correlationId) {
    StoreLegacySyncJob job = StoreLegacySyncJob.create(storeId, type, null, correlationId);
    entityManager.persist(job);
    entityManager.flush();
    return job;
  }

  private StoreLegacySyncJob reloadJob(UUID jobId) {
    entityManager.clear();
    return entityManager.find(StoreLegacySyncJob.class, jobId);
  }
}
