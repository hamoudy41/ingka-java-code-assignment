package com.fulfilment.application.monolith.stores.domain.exceptions;

import lombok.Getter;

@Getter
public class StoreNotFoundException extends RuntimeException {

  private final Long storeId;

  public StoreNotFoundException(Long storeId) {
    super("Store with id of " + storeId + " does not exist.");
    this.storeId = storeId;
  }
}

