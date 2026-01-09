package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseResourceTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  void cleanup() {
    warehouseRepository.delete("businessUnitCode LIKE ?1", "MWH.%");
  }

  @Test
  @DisplayName("GET /warehouse should return list of warehouses")
  void testGetAllWarehouses() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200);
  }

  @Test
  @DisplayName("POST /warehouse should create warehouse and return 201")
  void testCreateWarehouse() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201)
        .body("businessUnitCode", is(businessUnitCode))
        .body("location", is("AMSTERDAM-002"))
        .body("capacity", is(50))
        .body("stock", is(10))
        .body("creationAt", notNullValue());
  }

  @Test
  @DisplayName("POST /warehouse should reject warehouse with id set (Bean Validation)")
  void testCreateWarehouseWithIdSet() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"id\":\"test\",\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /warehouse should reject warehouse with null business unit code (Bean Validation)")
  void testCreateWarehouseWithNullBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":null,\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":10}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /warehouse should reject warehouse with negative capacity (Bean Validation)")
  void testCreateWarehouseWithNegativeCapacity() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":-1,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /warehouse should reject warehouse with negative stock (Bean Validation)")
  void testCreateWarehouseWithNegativeStock() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":-1}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("exceptionType", containsString("ConstraintViolationException"))
        .body("error", notNullValue());
  }

  @Test
  @DisplayName("POST /warehouse should reject duplicate business unit code")
  void testCreateWarehouseWithDuplicateBusinessUnitCode() {
    String businessUnitCode = "MWH.DUPLICATE." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(409)
        .body("exceptionType", containsString("DuplicateBusinessUnitCodeException"));
  }

  @Test
  @DisplayName("POST /warehouse should reject invalid location")
  void testCreateWarehouseWithInvalidLocation() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"INVALID-LOCATION\",\"capacity\":30,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("LocationNotFoundException"));
  }

  @Test
  @DisplayName("POST /warehouse should reject when capacity exceeds location max")
  void testCreateWarehouseWithCapacityExceedingLocationMax() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":80,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("LocationCapacityExceededException"));
  }

  @Test
  @DisplayName("POST /warehouse should reject when stock exceeds capacity")
  void testCreateWarehouseWithStockExceedingCapacity() {
    String businessUnitCode = "MWH.TEST." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":35}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InsufficientCapacityException"));
  }

  @Test
  @DisplayName("GET /warehouse/{id} should return warehouse by business unit code")
  void testGetWarehouseById() {
    String businessUnitCode = "MWH.GET." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 10);
    
    given()
        .when()
        .get("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(200)
        .body("businessUnitCode", is(businessUnitCode))
        .body("location", is("AMSTERDAM-002"))
        .body("capacity", is(30))
        .body("stock", is(10));
  }

  @Test
  @DisplayName("GET /warehouse/{id} should return 404 when warehouse not found")
  void testGetWarehouseNotFound() {
    given()
        .when()
        .get("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WarehouseNotFoundException"));
  }

  @Test
  @DisplayName("PUT /warehouse/{id} should replace warehouse")
  void testReplaceWarehouse() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 20);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}")
        .when()
        .put("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(200)
        .body("businessUnitCode", is(businessUnitCode))
        .body("location", is("AMSTERDAM-002"))
        .body("capacity", is(50))
        .body("stock", is(20));
  }

  @Test
  @DisplayName("PUT /warehouse/{id} should reject when stock mismatch")
  void testReplaceWarehouseWithStockMismatch() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 20);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":25}")
        .when()
        .put("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("StockMismatchException"));
  }

  @Test
  @DisplayName("PUT /warehouse/{id} should reject when capacity insufficient for existing stock")
  void testReplaceWarehouseWithInsufficientCapacity() {
    String businessUnitCode = "MWH.REPLACE." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 20);
    
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":15,\"stock\":20}")
        .when()
        .put("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InsufficientCapacityException"));
  }

  @Test
  @DisplayName("PUT /warehouse/{id} should return 404 when warehouse not found")
  void testReplaceWarehouseNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":10}")
        .when()
        .put("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WarehouseNotFoundException"));
  }

  @Test
  @DisplayName("DELETE /warehouse/{id} should archive warehouse")
  void testArchiveWarehouse() {
    String businessUnitCode = "MWH.ARCHIVE." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 10);
    
    given()
        .when()
        .delete("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(204);
    
    Warehouse archived = verifyArchived(businessUnitCode);
    assertNotNull(archived.getArchivedAt(), "Then archived timestamp should be set");
  }

  @Test
  @DisplayName("DELETE /warehouse/{id} should return 404 when warehouse not found")
  void testArchiveWarehouseNotFound() {
    given()
        .when()
        .delete("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WarehouseNotFoundException"));
  }

  @Test
  @DisplayName("DELETE /warehouse/{id} should reject when warehouse already archived")
  void testArchiveWarehouseAlreadyArchived() {
    String businessUnitCode = "MWH.ARCHIVE." + System.currentTimeMillis();
    createWarehouseWithAvailableLocation(businessUnitCode, 10);
    
    given()
        .when()
        .delete("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(204);
    
    given()
        .when()
        .delete("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(409)
        .body("exceptionType", containsString("WarehouseAlreadyArchivedException"));
  }

  @Test
  @DisplayName("POST /warehouse commits to database before returning")
  void testCreateCommitsToDatabase() {
    String businessUnitCode = "MWH.COMMIT." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201);
    
    Warehouse created = verifyCommitted(businessUnitCode);
    assertNotNull(created, "Then warehouse should be committed to database");
    assertEquals(businessUnitCode, created.getBusinessUnitCode(), "Then business unit code should match");
    assertEquals("AMSTERDAM-002", created.getLocation(), "Then location should match");
    assertEquals(50, created.getCapacity(), "Then capacity should match");
    assertEquals(10, created.getStock(), "Then stock should match");
  }

  @Test
  @DisplayName("POST /warehouse should return 400 for malformed JSON (Bean Validation)")
  void testCreateWarehouseWithMalformedJson() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":\"MWH.TEST\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":10")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("POST /warehouse should return 400 when capacity is null (Bean Validation)")
  void testCreateWarehouseWithNullCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":\"MWH.TEST\",\"location\":\"AMSTERDAM-002\",\"stock\":10}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  private String createWarehouse(String businessUnitCode, String location, int capacity, int stock) {
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":%d,\"stock\":%d}",
            businessUnitCode, location, capacity, stock))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201);
    return businessUnitCode;
  }

  private void createWarehouseWithAvailableLocation(String businessUnitCode, int stock) {
    createWarehouse(businessUnitCode, "AMSTERDAM-002", 30, stock);
  }

  private Warehouse verifyCommitted(String businessUnitCode) {
      return warehouseRepository.findByBusinessUnitCode(businessUnitCode);
  }

  private Warehouse verifyArchived(String businessUnitCode) {
      return warehouseRepository.findByBusinessUnitCode(businessUnitCode);
  }
}

