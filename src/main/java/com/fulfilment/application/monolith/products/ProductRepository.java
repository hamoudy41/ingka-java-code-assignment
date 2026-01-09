package com.fulfilment.application.monolith.products;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository for product persistence operations.
 * Provides Panache-based data access for Product entities.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {}
