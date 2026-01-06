package com.fulfilment.application.monolith.warehouses.adapters.database;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouse")
@Cacheable
@Getter
@Setter
@NoArgsConstructor
public class DbWarehouse {

  @Id
  @GeneratedValue
  private Long id;

  private String businessUnitCode;

  private String location;

  private Integer capacity;

  private Integer stock;

  private LocalDateTime createdAt;

  private LocalDateTime archivedAt;
}
