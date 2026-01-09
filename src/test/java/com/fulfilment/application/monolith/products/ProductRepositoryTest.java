package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.panache.common.Sort;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class ProductRepositoryTest {

  @Inject
  ProductRepository productRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    productRepository.delete("name LIKE ?1", "REPO.TEST.%");
  }

  @Test
  @DisplayName("Should persist and find product by id")
  void shouldPersistAndFindProductById() {
    Product product = new Product();
    product.setName("REPO.TEST." + System.currentTimeMillis());
    product.setDescription("Test Description");
    product.setPrice(new BigDecimal("99.99"));
    product.setStock(50);

    productRepository.persist(product);
    assertNotNull(product.getId(), "Then product id should be set after persist");

    Product found = productRepository.findById(product.getId());
    assertNotNull(found, "Then product should be found by id");
    assertEquals(product.getName(), found.getName(), "Then name should match");
    assertEquals(product.getDescription(), found.getDescription(), "Then description should match");
    assertEquals(product.getPrice(), found.getPrice(), "Then price should match");
    assertEquals(product.getStock(), found.getStock(), "Then stock should match");
  }

  @Test
  @DisplayName("Should list all products with sorting")
  void shouldListAllProductsWithSorting() {
    Product product1 = new Product();
    product1.setName("REPO.TEST.B." + System.currentTimeMillis());
    product1.setStock(10);
    productRepository.persist(product1);

    Product product2 = new Product();
    product2.setName("REPO.TEST.A." + System.currentTimeMillis());
    product2.setStock(20);
    productRepository.persist(product2);

    List<Product> products = productRepository.listAll(Sort.by("name"));
    assertNotNull(products, "Then products list should not be null");
    assertEquals(2, products.stream().filter(p -> p.getName().startsWith("REPO.TEST.")).count(), 
        "Then should have 2 test products");
  }

  @Test
  @DisplayName("Should delete product")
  void shouldDeleteProduct() {
    Product product = new Product();
    product.setName("REPO.TEST.DELETE." + System.currentTimeMillis());
    product.setStock(10);
    productRepository.persist(product);

    Long id = product.getId();
    assertNotNull(id, "Then product id should be set");

    productRepository.delete(product);

    Product deleted = productRepository.findById(id);
    assertNull(deleted, "Then product should be deleted");
  }

  @Test
  @DisplayName("Should handle product with null description")
  void shouldHandleProductWithNullDescription() {
    Product product = new Product();
    product.setName("REPO.TEST.NULL.DESC." + System.currentTimeMillis());
    product.setDescription(null);
    product.setStock(10);
    productRepository.persist(product);

    Product found = productRepository.findById(product.getId());
    assertNotNull(found, "Then product should be found");
    assertNull(found.getDescription(), "Then description should be null");
  }

  @Test
  @DisplayName("Should handle product with null price")
  void shouldHandleProductWithNullPrice() {
    Product product = new Product();
    product.setName("REPO.TEST.NULL.PRICE." + System.currentTimeMillis());
    product.setPrice(null);
    product.setStock(10);
    productRepository.persist(product);

    Product found = productRepository.findById(product.getId());
    assertNotNull(found, "Then product should be found");
    assertNull(found.getPrice(), "Then price should be null");
  }

  @Test
  @DisplayName("Should handle product with zero stock")
  void shouldHandleProductWithZeroStock() {
    Product product = new Product();
    product.setName("REPO.TEST.ZERO.STOCK." + System.currentTimeMillis());
    product.setStock(0);
    productRepository.persist(product);

    Product found = productRepository.findById(product.getId());
    assertNotNull(found, "Then product should be found");
    assertEquals(0, found.getStock(), "Then stock should be zero");
  }

  @Test
  @DisplayName("Should handle product with negative stock")
  void shouldHandleProductWithNegativeStock() {
    Product product = new Product();
    product.setName("REPO.TEST.NEGATIVE." + System.currentTimeMillis());
    product.setStock(-5);
    productRepository.persist(product);

    Product found = productRepository.findById(product.getId());
    assertNotNull(found, "Then product should be found");
    assertEquals(-5, found.getStock(), "Then stock should be negative");
  }

  @Test
  @DisplayName("Should update product fields")
  void shouldUpdateProductFields() {
    Product product = new Product();
    product.setName("REPO.TEST.UPDATE." + System.currentTimeMillis());
    product.setDescription("Original");
    product.setPrice(new BigDecimal("10.00"));
    product.setStock(10);
    productRepository.persist(product);

    product.setDescription("Updated");
    product.setPrice(new BigDecimal("20.00"));
    product.setStock(20);
    productRepository.persist(product);

    Product updated = productRepository.findById(product.getId());
    assertEquals("Updated", updated.getDescription(), "Then description should be updated");
    assertEquals(new BigDecimal("20.00"), updated.getPrice(), "Then price should be updated");
    assertEquals(20, updated.getStock(), "Then stock should be updated");
  }
}
