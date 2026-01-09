package com.fulfilment.application.monolith.stores.adapters.database;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a store using Panache.
 * Stores have a unique name and track product stock quantity.
 * Includes optimistic locking via version field.
 */
@Entity
@Cacheable
@Getter
@Setter
@NoArgsConstructor
public class Store extends PanacheEntity {

  @Column(length = 40, unique = true)
  private String name;

  private int quantityProductsInStock;

  @Version
  private Long version;

  public Long getId() {
    return id;
  }
}

