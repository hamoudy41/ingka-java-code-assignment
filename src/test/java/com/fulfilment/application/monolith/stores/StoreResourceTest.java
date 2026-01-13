package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.domain.exceptions.StoreAlreadyExistsException;
import com.fulfilment.application.monolith.stores.domain.exceptions.InvalidStoreRequestException;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import com.fulfilment.application.monolith.stores.domain.exceptions.StoreNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @BeforeEach
  @Transactional
  void cleanup() {
    Store.delete("name LIKE ?1", "Test Store%");
    Store.delete("name LIKE ?1", "Original Store%");
    Store.delete("name LIKE ?1", "Updated Store%");
  }

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
  @DisplayName("POST /stores should return 409 when store name already exists")
  public void testCreateStoreDuplicateName() {
    String name = "Test Store DUP " + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"quantityProductsInStock\":10}", name))
        .when()
        .post("/stores")
        .then()
        .statusCode(201);
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"quantityProductsInStock\":10}", name))
        .when()
        .post("/stores")
        .then()
        .statusCode(409)
        .body("exceptionType", containsString(StoreAlreadyExistsException.class.getName()))
        .body("error", containsString("already exists"));
  }

  @Test
  @DisplayName("POST /stores should reject store with id set (Bean Validation)")
  public void testCreateStoreWithIdSet() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"Test Store\",\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /stores should reject store with null name (Bean Validation)")
  public void testCreateStoreWithNullName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":null,\"quantityProductsInStock\":10}")
        .when()
        .post("/stores")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /stores should reject store with negative stock (Bean Validation)")
  public void testCreateStoreWithNegativeStock() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Test Store\",\"quantityProductsInStock\":-1}")
        .when()
        .post("/stores")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
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
  @DisplayName("PUT /stores/{id} should return 409 when updating store name to existing name")
  public void testUpdateStoreDuplicateName() {
    Long store1Id = createStore("Test Store DUP1 " + System.currentTimeMillis(), 10);
    String store2Name = "Test Store DUP2 " + System.currentTimeMillis();
    createStore(store2Name, 10);

    given()
        .contentType(ContentType.JSON)
        .body(String.format("{\"name\":\"%s\",\"quantityProductsInStock\":10}", store2Name))
        .when()
        .put("/stores/" + store1Id)
        .then()
        .statusCode(409)
        .body("exceptionType", containsString(StoreAlreadyExistsException.class.getName()))
        .body("error", containsString("already exists"));
  }

  @Test
  @DisplayName("PUT /stores/{id} should return 400 when request body is null (Bean Validation)")
  public void testUpdateStoreWithNullBody() {
    Long storeId = createStore("Test Store NULLBODY " + System.currentTimeMillis(), 10);

    given()
        .contentType(ContentType.JSON)
        .body("null")
        .when()
        .put("/stores/" + storeId)
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"));
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
  @DisplayName("PUT /stores/{id} should reject null name (Bean Validation)")
  public void testUpdateStoreWithNullName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":null,\"quantityProductsInStock\":20}")
        .when()
        .put("/stores/1")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"));
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

  @Test
  @DisplayName("PATCH /stores/{id} should update only name when provided")
  void shouldPatchOnlyName() {
    String originalName = "Original Store " + System.currentTimeMillis();
    Long storeId = createStore(originalName, 50);
    String updatedName = "Updated Store " + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + updatedName + "\"}")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(updatedName))
        .body("quantityProductsInStock", is(50));
    
    verifyCommitted(storeId, updatedName, 50);
  }

  @Test
  @DisplayName("PATCH /stores/{id} should return 409 when patching store name to existing name")
  void shouldReturn409WhenPatchingDuplicateName() {
    Long store1Id = createStore("Test Store PATCHDUP1 " + System.currentTimeMillis(), 10);
    String store2Name = "Test Store PATCHDUP2 " + System.currentTimeMillis();
    createStore(store2Name, 10);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + store2Name + "\"}")
        .when()
        .patch("/stores/" + store1Id)
        .then()
        .statusCode(409)
        .body("exceptionType", containsString(StoreAlreadyExistsException.class.getName()))
        .body("error", containsString("already exists"));
  }

  @Test
  @DisplayName("PATCH /stores/{id} should return 400 when request body is null (Bean Validation)")
  void shouldReturn400WhenPatchBodyIsNull() {
    Long storeId = createStore("Test Store PATCHNULL " + System.currentTimeMillis(), 10);

    given()
        .contentType(ContentType.JSON)
        .body("null")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"));
  }

  @Test
  @DisplayName("PATCH /stores/{id} should update only stock when provided")
  void shouldPatchOnlyStock() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 50);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":75}")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(name))
        .body("quantityProductsInStock", is(75));
    
    verifyCommitted(storeId, name, 75);
  }

  @Test
  @DisplayName("PATCH /stores/{id} should reject when blank name provided (Bean Validation)")
  void shouldRejectWhenBlankNameProvided() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 50);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"   \"}")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"));
  }

  @Test
  @DisplayName("PATCH /stores/{id} should return store unchanged when no fields provided")
  void shouldReturnUnchangedWhenNoFieldsProvided() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 50);
    
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .patch("/stores/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(name))
        .body("quantityProductsInStock", is(50));
  }

  @Test
  @DisplayName("GET /stores/{id} should return store by id")
  void shouldReturnStoreById() {
    String name = "Test Store " + System.currentTimeMillis();
    Long storeId = createStore(name, 100);
    
    given()
        .when()
        .get("/stores/" + storeId)
        .then()
        .statusCode(200)
        .body("id", is(storeId.intValue()))
        .body("name", is(name))
        .body("quantityProductsInStock", is(100));
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

  @Test
  @DisplayName("POST /stores should return 400 for malformed JSON (Bean Validation)")
  void testCreateStoreWithMalformedJson() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Test Store\",\"quantityProductsInStock\":10")
        .when()
        .post("/stores")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("LegacySyncException should create with message")
  void shouldCreateLegacySyncExceptionWithMessage() {
    String message = "Sync failed";
    LegacySyncException exception = new LegacySyncException(message);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
  }

  @Test
  @DisplayName("LegacySyncException should create with message and cause")
  void shouldCreateLegacySyncExceptionWithCause() {
    String message = "Sync failed";
    Throwable cause = new RuntimeException("IO error");
    LegacySyncException exception = new LegacySyncException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
    assertEquals(cause, exception.getCause(), "Then cause should match");
  }

  @Test
  @DisplayName("InvalidStoreRequestException should create with message")
  void shouldCreateInvalidStoreRequestExceptionWithMessage() {
    String message = "Invalid request";
    InvalidStoreRequestException exception = new InvalidStoreRequestException(message);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
  }

  @Test
  @DisplayName("InvalidStoreRequestException should create with message and cause")
  void shouldCreateInvalidStoreRequestExceptionWithCause() {
    String message = "Invalid request";
    Throwable cause = new IllegalArgumentException("Bad data");
    InvalidStoreRequestException exception = new InvalidStoreRequestException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Then message should match");
    assertEquals(cause, exception.getCause(), "Then cause should match");
  }

  @Test
  @DisplayName("StoreNotFoundException should create with store id")
  void shouldCreateStoreNotFoundException() {
    Long storeId = 123L;
    StoreNotFoundException exception = new StoreNotFoundException(storeId);
    
    assertEquals(storeId, exception.getStoreId(), "Then store id should match");
    assertTrue(exception.getMessage().contains(storeId.toString()), "Then message should contain store id");
  }
}

