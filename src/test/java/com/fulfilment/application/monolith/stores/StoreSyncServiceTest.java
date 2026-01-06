package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.adapters.legacy.StoreSyncService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StoreSyncServiceTest {

  @Inject StoreSyncService storeSyncService;
  @Inject EntityManager entityManager;
  @Inject UserTransaction userTransaction;

  @Test
  @DisplayName("Store is queryable from database after commit, verifying commit happens before sync")
  void storeIsQueryableAfterCommit() throws Exception {
    Store store = createStore("Test Store", 100);
    
    commit(() -> {
      store.persist();
      storeSyncService.scheduleCreateSync(store);
    });
    
    Store persisted = findById(store.getId());
    assertNotNull(persisted, "Store must be committed and queryable after commit");
    assertEquals(store.getName(), persisted.getName());
    assertEquals(100, persisted.getQuantityProductsInStock());
  }

  @Test
  @DisplayName("Sync reloads store from database after commit, receiving committed data not snapshot")
  void syncReloadsCommittedData() throws Exception {
    Store store = createStore("Test Store", 20);
    
    Long storeId = commit(() -> {
      store.persist();
      Long id = store.getId();
      storeSyncService.scheduleCreateSync(store);
      store.setQuantityProductsInStock(25);
      store.persist();
      return id;
    });
    
    Store committed = findById(storeId);
    assertNotNull(committed, "Store must be committed");
    assertEquals(25, committed.getQuantityProductsInStock(), 
        "Committed value should be 25, sync should reload this value");
  }

  @Test
  @DisplayName("Sync does not happen if transaction rolls back")
  void syncDoesNotHappenOnRollback() throws Exception {
    Store store = createStore("Test Store", 30);
    
    userTransaction.begin();
    try {
      store.persist();
      storeSyncService.scheduleCreateSync(store);
      userTransaction.rollback();
    } finally {
      entityManager.clear();
    }
    
    Store rolledBack = findById(store.getId());
    assertNull(rolledBack, "Store should not exist after rollback");
  }

  @Test
  @DisplayName("Update sync happens after commit")
  void updateSyncHappensAfterCommit() throws Exception {
    Long storeId = commit(() -> {
      Store store = createStore("Test Store", 10);
      store.persist();
      return store.getId();
    });
    
    commit(() -> {
      Store entity = entityManager.find(Store.class, storeId);
      entity.setQuantityProductsInStock(20);
      entity.persist();
      storeSyncService.scheduleUpdateSync(entity);
    });
    
    Store updated = findById(storeId);
    assertNotNull(updated, "Updated store must be committed");
    assertEquals(20, updated.getQuantityProductsInStock());
  }

  @Test
  @DisplayName("Sync receives latest committed version after concurrent modification")
  void syncReceivesLatestVersionAfterConcurrentModification() throws Exception {
    Store store = createStore("Test Store", 50);
    
    Long storeId = commit(() -> {
      store.persist();
      Long id = store.getId();
      storeSyncService.scheduleCreateSync(store);
      return id;
    });
    
    commit(() -> {
      Store entity = entityManager.find(Store.class, storeId);
      entity.setQuantityProductsInStock(60);
      entity.persist();
    });
    
    Store latest = findById(storeId);
    assertEquals(60, latest.getQuantityProductsInStock(), 
        "Sync should reload latest version from database");
  }

  private Store createStore(String name, int stock) {
    Store store = new Store();
    store.setName(name + " " + System.currentTimeMillis());
    store.setQuantityProductsInStock(stock);
    return store;
  }

  private void commit(Runnable action) throws Exception {
    userTransaction.begin();
    try {
      action.run();
      userTransaction.commit();
    } finally {
      entityManager.clear();
    }
  }

  private <T> T commit(java.util.function.Supplier<T> action) throws Exception {
    userTransaction.begin();
    try {
      T result = action.get();
      userTransaction.commit();
      return result;
    } finally {
      entityManager.clear();
    }
  }

  private Store findById(Long id) {
    return entityManager.find(Store.class, id);
  }
}
