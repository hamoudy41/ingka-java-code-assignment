package com.fulfilment.application.monolith.stores.adapters.legacy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class LegacyStoreManagerGatewayTest {

  @Inject
  LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  @Transactional
  void cleanup() {
    Store.delete("name LIKE ?1", "Test Store%");
  }

  @Test
  @DisplayName("Should create store on legacy system")
  void shouldCreateStoreOnLegacySystem() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(10);
    store.persist();

    assertDoesNotThrow(() -> {
      legacyStoreManagerGateway.createStoreOnLegacySystem(store);
    }, "Then store should be created on legacy system without exception");
  }

  @Test
  @DisplayName("Should update store on legacy system")
  void shouldUpdateStoreOnLegacySystem() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(10);
    store.persist();

    assertDoesNotThrow(() -> {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
    }, "Then store should be updated on legacy system without exception");
  }

  @Test
  @DisplayName("Should handle store with zero stock")
  void shouldHandleStoreWithZeroStock() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(0);
    store.persist();

    assertDoesNotThrow(() -> {
      legacyStoreManagerGateway.createStoreOnLegacySystem(store);
    }, "Then store with zero stock should be handled");
  }

  @Test
  @DisplayName("Should handle store with large stock value")
  void shouldHandleStoreWithLargeStockValue() {
    Store store = new Store();
    store.setName("Test Store " + System.currentTimeMillis());
    store.setQuantityProductsInStock(Integer.MAX_VALUE);
    store.persist();

    assertDoesNotThrow(() -> {
      legacyStoreManagerGateway.createStoreOnLegacySystem(store);
    }, "Then store with large stock value should be handled");
  }

  @Test
  @DisplayName("Should handle store with special characters in name")
  void shouldHandleStoreWithSpecialCharactersInName() {
    Store store = new Store();
    store.setName("Test Store !@#$%^&*() " + System.currentTimeMillis());
    store.setQuantityProductsInStock(10);
    store.persist();

    assertDoesNotThrow(() -> {
      legacyStoreManagerGateway.createStoreOnLegacySystem(store);
    }, "Then store with special characters in name should be handled");
  }
}
