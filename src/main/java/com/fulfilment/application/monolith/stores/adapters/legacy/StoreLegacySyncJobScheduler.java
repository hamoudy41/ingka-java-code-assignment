package com.fulfilment.application.monolith.stores.adapters.legacy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.jbosslog.JBossLog;

/**
 * Minimal scheduler for retrying legacy sync jobs without adding new dependencies.
 */
@ApplicationScoped
@JBossLog
public class StoreLegacySyncJobScheduler {

  private static final long POLL_INTERVAL_MS = 200;

  @Inject StoreLegacySyncJobWorker worker;

  private ScheduledExecutorService executor;

  @PostConstruct
  void start() {
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "store-legacy-sync-job-worker");
      t.setDaemon(true);
      return t;
    });
    executor.scheduleWithFixedDelay(() -> {
      try {
        worker.processDueJobs();
      } catch (Exception e) {
        log.error("Error processing legacy sync jobs", e);
      }
    }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  @PreDestroy
  void stop() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }
}

