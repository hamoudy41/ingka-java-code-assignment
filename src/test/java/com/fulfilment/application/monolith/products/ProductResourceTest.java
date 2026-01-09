package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Transactional
class ProductResourceTest {

  @Inject
  ProductRepository productRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    productRepository.delete("name LIKE ?1", "TEST.%");
  }

  @Test
  @DisplayName("POST /product should reject product with id set")
  void shouldRejectProductWithIdSet() {
    String uniqueName = "TEST.EXISTING." + System.currentTimeMillis();
    Long existingId = createTestProductViaApi(uniqueName, "Test", 10.0, 5);
    String newName = "TEST.CREATE." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"id\":%d,\"name\":\"%s\",\"description\":\"Test\",\"price\":10.0,\"stock\":5}", existingId, newName))
        .when()
        .post("/product")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("WebApplicationException"))
        .body("error", containsString("Id was invalidly set"));
  }

  @Test
  @DisplayName("GET /product should return list of products sorted by name")
  void shouldReturnListOfProducts() {
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200);
  }

  @Test
  @DisplayName("GET /product/{id} should return product by id")
  void shouldReturnProductById() {
    String uniqueName = "TEST.GET." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "Description", 10.50, 5);
    
    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", containsString("TEST.GET"))
        .body("description", is("Description"))
        .body("price", is(10.50f))
        .body("stock", is(5));
  }

  @Test
  @DisplayName("GET /product/{id} should return 404 when product not found")
  void shouldReturn404WhenProductNotFound() {
    given()
        .when()
        .get("/product/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WebApplicationException"))
        .body("error", containsString("does not exist"));
  }

  @Test
  @DisplayName("POST /product should create product and return 201")
  void shouldCreateProduct() {
    String uniqueName = "TEST.CREATE." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Test Description\",\"price\":15.75,\"stock\":10}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", is(uniqueName))
        .body("description", is("Test Description"))
        .body("price", is(15.75f))
        .body("stock", is(10))
        .body("id", notNullValue());
  }


  @Test
  @DisplayName("POST /product should handle product with null description")
  void shouldHandleProductWithNullDescription() {
    String uniqueName = "TEST.CREATE." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":null,\"price\":10.0,\"stock\":5}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", is(uniqueName));
  }

  @Test
  @DisplayName("POST /product should handle product with null price")
  void shouldHandleProductWithNullPrice() {
    String uniqueName = "TEST.CREATE." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Test\",\"price\":null,\"stock\":5}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", is(uniqueName));
  }

  @Test
  @DisplayName("PUT /product/{id} should update product")
  void shouldUpdateProduct() {
    String uniqueName = "TEST.UPDATE." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "Original", 10.0, 5);
    String updatedName = "TEST.UPDATED." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Updated Description\",\"price\":20.0,\"stock\":15}", updatedName))
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", is(updatedName))
        .body("description", is("Updated Description"))
        .body("price", is(20.0f))
        .body("stock", is(15));
  }

  @Test
  @DisplayName("PUT /product/{id} should reject update with null name")
  void shouldRejectUpdateWithNullName() {
    String uniqueName = "TEST.UPDATE." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "Original", 10.0, 5);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":null,\"description\":\"Updated\",\"price\":20.0,\"stock\":15}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("WebApplicationException"))
        .body("error", containsString("Product Name was not set"));
  }

  @Test
  @DisplayName("PUT /product/{id} should return 404 when product not found")
  void shouldReturn404WhenUpdatingNonExistentProduct() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"TEST.UPDATE\",\"description\":\"Test\",\"price\":10.0,\"stock\":5}")
        .when()
        .put("/product/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WebApplicationException"))
        .body("error", containsString("does not exist"));
  }

  @Test
  @DisplayName("PUT /product/{id} should handle update with null description")
  void shouldHandleUpdateWithNullDescription() {
    String uniqueName = "TEST.UPDATE." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "Original", 10.0, 5);
    String updatedName = "TEST.UPDATED." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":null,\"price\":20.0,\"stock\":15}", updatedName))
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", is(updatedName));
  }

  @Test
  @DisplayName("PUT /product/{id} should handle update with null price")
  void shouldHandleUpdateWithNullPrice() {
    String uniqueName = "TEST.UPDATE." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "Original", 10.0, 5);
    String updatedName = "TEST.UPDATED." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Updated\",\"price\":null,\"stock\":15}", updatedName))
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", is(updatedName));
  }

  @Test
  @DisplayName("DELETE /product/{id} should delete product")
  void shouldDeleteProduct() {
    String uniqueName = "TEST.DELETE." + System.currentTimeMillis();
    Long productId = createTestProductViaApi(uniqueName, "To Delete", 10.0, 5);
    
    given()
        .when()
        .delete("/product/" + productId)
        .then()
        .statusCode(204);
    
    Product deleted = productRepository.findById(productId);
    assertEquals(null, deleted, "Then product should be deleted");
  }

  @Test
  @DisplayName("DELETE /product/{id} should return 404 when product not found")
  void shouldReturn404WhenDeletingNonExistentProduct() {
    given()
        .when()
        .delete("/product/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WebApplicationException"))
        .body("error", containsString("does not exist"));
  }

  @Test
  @DisplayName("Should handle product with zero stock")
  void shouldHandleProductWithZeroStock() {
    String uniqueName = "TEST.ZERO." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Test\",\"price\":10.0,\"stock\":0}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("stock", is(0));
  }

  @Test
  @DisplayName("Should handle product with negative stock")
  void shouldHandleProductWithNegativeStock() {
    String uniqueName = "TEST.NEGATIVE." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Test\",\"price\":10.0,\"stock\":-5}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("stock", is(-5));
  }

  @Test
  @DisplayName("Should handle product with very large price")
  void shouldHandleProductWithVeryLargePrice() {
    String uniqueName = "TEST.LARGE." + System.currentTimeMillis();
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"description\":\"Test\",\"price\":999999.99,\"stock\":10}", uniqueName))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("price", is(999999.99f));
  }

  private Long createTestProductViaApi(String name, String description, Double price, int stock) {
    StringBuilder body = new StringBuilder("{");
    body.append("\"name\":\"").append(name).append("\"");
    if (description != null) {
      body.append(",\"description\":\"").append(description.replace("\"", "\\\"")).append("\"");
    } else {
      body.append(",\"description\":null");
    }
    if (price != null) {
      body.append(",\"price\":").append(price);
    } else {
      body.append(",\"price\":null");
    }
    body.append(",\"stock\":").append(stock);
    body.append("}");
    
    return ((Number) given()
        .contentType(ContentType.JSON)
        .body(body.toString())
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .path("id")).longValue();
  }
}
