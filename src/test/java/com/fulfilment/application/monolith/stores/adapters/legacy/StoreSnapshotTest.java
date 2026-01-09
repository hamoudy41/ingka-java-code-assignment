package com.fulfilment.application.monolith.stores.adapters.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class StoreSnapshotTest {

  @BeforeEach
  @Transactional
  void cleanup() {
    Store.delete("name LIKE ?1", "Test Store%");
  }

  @Test
  @DisplayName("Should create snapshot from valid store")
  void shouldCreateSnapshotFromValidStore() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(10);
    store.persist();

    StoreSnapshot snapshot = StoreSnapshot.from(store);

    assertNotNull(snapshot, "Then snapshot should be created");
    assertEquals(store.getId(), snapshot.id(), "Then id should match");
    assertEquals(store.getName(), snapshot.name(), "Then name should match");
    assertEquals(store.getQuantityProductsInStock(), snapshot.quantityProductsInStock(), "Then stock should match");
    assertEquals(store.getVersion(), snapshot.version(), "Then version should match");
  }

  @Test
  @DisplayName("Should throw exception when creating snapshot from null store")
  void shouldThrowExceptionWhenStoreIsNull() {
    assertThrows(IllegalArgumentException.class, () -> {
      StoreSnapshot.from(null);
    }, "Then exception should be thrown when store is null");
  }

  @Test
  @DisplayName("Should throw exception when store id is null")
  void shouldThrowExceptionWhenStoreIdIsNull() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());

    assertThrows(IllegalArgumentException.class, () -> {
      StoreSnapshot.from(store);
    }, "Then exception should be thrown when store id is null");
  }

  @Test
  @DisplayName("Should throw exception when snapshot id is null")
  void shouldThrowExceptionWhenSnapshotIdIsNull() {
    assertThrows(NullPointerException.class, () -> {
      new StoreSnapshot(null, "Test Store", 10, 0L);
    }, "Then exception should be thrown when snapshot id is null");
  }

  @Test
  @DisplayName("Should throw exception when snapshot name is null")
  void shouldThrowExceptionWhenSnapshotNameIsNull() {
    assertThrows(NullPointerException.class, () -> {
      new StoreSnapshot(1L, null, 10, 0L);
    }, "Then exception should be thrown when snapshot name is null");
  }

  @Test
  @DisplayName("Should create snapshot with version from persisted store")
  void shouldCreateSnapshotWithVersionFromPersistedStore() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(10);
    store.persist();

    StoreSnapshot snapshot = StoreSnapshot.from(store);

    assertNotNull(snapshot, "Then snapshot should be created");
    assertEquals(store.getId(), snapshot.id(), "Then id should match");
    assertEquals(store.getVersion(), snapshot.version(), "Then version should match store version");
  }
}
