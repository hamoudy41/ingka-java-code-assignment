package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @Test
  @DisplayName("POST /stores should create store and return 201")
  public void testCreateStore() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Test Store\",\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(201)
        .body("name", is("Test Store"))
        .body("quantityProductsInStock", is(10))
        .body("id", notNullValue());
  }

  @Test
  @DisplayName("POST /stores should reject store with id set")
  public void testCreateStoreWithIdSet() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"Test Store\",\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InvalidStoreRequestException"))
        .body("error", containsString("Id was invalidly set"));
  }

  @Test
  @DisplayName("POST /stores should reject store with null name")
  public void testCreateStoreWithNullName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":null,\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InvalidStoreRequestException"))
        .body("error", containsString("name is required"));
  }

  @Test
  @DisplayName("POST /stores should reject store with negative stock")
  public void testCreateStoreWithNegativeStock() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Test Store\",\"quantityProductsInStock\":-1}")
        .when()
        .post("/stores")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InvalidStoreRequestException"))
        .body("error", containsString("cannot be negative"));
  }

  @Test
  @DisplayName("PUT /stores/{id} should update store")
  public void testUpdateStore() {
    Long storeId = ((Number) given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Original Store\",\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(201)
        .extract()
        .path("id")).longValue();

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated Store\",\"quantityProductsInStock\":25}")
        .when()
        .put("/stores/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is("Updated Store"))
        .body("quantityProductsInStock", is(25))
        .body("id", is(storeId.intValue()));
  }

  @Test
  @DisplayName("PUT /stores/{id} should return 404 when store not found")
  public void testUpdateStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated Store\",\"quantityProductsInStock\":20}")
        .when()
        .put("/stores/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("StoreNotFoundException"));
  }

  @Test
  @DisplayName("PUT /stores/{id} should reject null name")
  public void testUpdateStoreWithNullName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":null,\"quantityProductsInStock\":20}")
        .when()
        .put("/stores/1")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InvalidStoreRequestException"));
  }

  @Test
  @DisplayName("PATCH /stores/{id} should return 404 when store not found")
  public void testPatchStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Patched Store\"}")
        .when()
        .patch("/stores/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("StoreNotFoundException"));
  }

  @Test
  @DisplayName("DELETE /stores/{id} should return 404 when store not found")
  public void testDeleteStoreNotFound() {
    given()
        .when()
        .delete("/stores/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("StoreNotFoundException"));
  }

  @Test
  @DisplayName("GET /stores should return list of stores")
  public void testGetAllStores() {
    given()
        .when()
        .get("/stores")
        .then()
        .statusCode(200);
  }

  @Test
  @DisplayName("GET /stores/{id} should return 404 when store not found")
  public void testGetStoreNotFound() {
    given()
        .when()
        .get("/stores/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("StoreNotFoundException"));
  }

  @Test
  @DisplayName("POST /stores commits to database before legacy sync")
  void createCommitsBeforeSync() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 100);
    verifyCommitted(storeId, name, 100);
  }

  @Test
  @DisplayName("PUT /stores/{id} commits to database before legacy sync")
  void updateCommitsBeforeSync() {
    String originalName = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(originalName, 50);
    String updatedName = "Updated Store " + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"quantityProductsInStock\":75}", updatedName))
        .when()
        .put("/stores/" + storeId)
        .then()
        .statusCode(200);
    
    verifyCommitted(storeId, updatedName, 75);
  }

  @Test
  @DisplayName("PATCH /stores/{id} commits to database before legacy sync")
  void patchCommitsBeforeSync() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 30);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":45}")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(200);
    
    verifyCommitted(storeId, name, 45);
  }

  private Long createStore(String name, int stock) {
    return ((Number) given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"quantityProductsInStock\":%d}", name, stock))
        .when()
        .post("/stores")
        .then()
        .statusCode(201)
        .extract()
        .path("id")).longValue();
  }

  private void verifyCommitted(Long storeId, String expectedName, int expectedStock) {
    Store store = Store.findById(storeId);
    assertNotNull(store, "Store must be committed to database");
    assertEquals(expectedName, store.getName());
    assertEquals(expectedStock, store.getQuantityProductsInStock());
  }
}

