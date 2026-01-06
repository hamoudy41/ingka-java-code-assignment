package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.Setter;

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
