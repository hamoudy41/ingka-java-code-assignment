package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Domain model representing a warehouse.
 * Contains business logic and validation rules for warehouse entities.
 */
@Getter
@Setter
public class Warehouse {

  private String businessUnitCode;

  private String location;

  private Integer capacity;

  private Integer stock;

  private ZonedDateTime creationAt;

  private ZonedDateTime archivedAt;
}
