package com.fulfilment.application.monolith.stores.adapters.legacy;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gateway for interacting with the legacy store management system.
 * Simulates legacy system integration by writing store data to temporary files.
 */
@ApplicationScoped
public class LegacyStoreManagerGateway {

  public void createStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      Path tempFile = Files.createTempFile(store.getName(), ".txt");
      String content = String.format("Store created. [ name =%s ] [ items on stock =%d]",
          store.getName(), store.getQuantityProductsInStock());
      Files.write(tempFile, content.getBytes());
      Files.delete(tempFile);
    } catch (Exception e) {
      throw new LegacySyncException("Failed to sync store to legacy system", e);
    }
  }
}

