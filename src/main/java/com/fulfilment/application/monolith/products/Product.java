package com.fulfilment.application.monolith.products;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a product.
 * Products have a unique name and optional description and price.
 */
@Entity
@Cacheable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 40, unique = true)
  private String name;

  @Column(nullable = true)
  private String description;

  @Column(precision = 10, scale = 2, nullable = true)
  private BigDecimal price;

  private int stock;

  public Product(String name) {
    this.name = name;
  }
}
