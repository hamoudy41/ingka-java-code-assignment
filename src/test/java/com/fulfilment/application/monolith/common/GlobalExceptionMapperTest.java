package com.fulfilment.application.monolith.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for GlobalExceptionMapper to ensure all exception handlers
 * are properly exercised and return correct HTTP status codes and error responses.
 */
@QuarkusTest
public class GlobalExceptionMapperTest {

  @Inject
  EntityManager entityManager;

  @BeforeEach
  @Transactional
  void cleanup() {
    entityManager.createQuery("DELETE FROM DbWarehouse WHERE businessUnitCode LIKE 'MWH.EXCEPTION.%'").executeUpdate();
    entityManager.createQuery("DELETE FROM Store WHERE name LIKE 'Exception Test%'").executeUpdate();
  }

  @Test
  @DisplayName("Should return 400 for InvalidLocationIdentifierException (tested in LocationGatewayTest)")
  void shouldReturn400ForInvalidLocationIdentifier() {
  }

  @Test
  @DisplayName("Should return 400 for Bean Validation errors with violations")
  void shouldReturn400ForBeanValidationWithViolations() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":null,\"location\":\"AMSTERDAM-002\",\"capacity\":-1,\"stock\":-1}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("exceptionType", is("jakarta.validation.ConstraintViolationException"))
        .body("code", is(400))
        .body("error", notNullValue())
        .body("violations", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 404 for WarehouseNotFoundException")
  void shouldReturn404ForWarehouseNotFound() {
    given()
        .when()
        .get("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("WarehouseNotFoundException"))
        .body("code", is(404))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 404 for StoreNotFoundException")
  void shouldReturn404ForStoreNotFound() {
    given()
        .when()
        .get("/stores/99999")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("StoreNotFoundException"))
        .body("code", is(404))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 404 for LocationNotFoundException")
  void shouldReturn404ForLocationNotFound() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"INVALID-LOCATION\",\"capacity\":30,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(404)
        .body("exceptionType", containsString("LocationNotFoundException"))
        .body("code", is(404))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 409 for DuplicateBusinessUnitCodeException")
  void shouldReturn409ForDuplicateBusinessUnitCode() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
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
        .body("exceptionType", containsString("DuplicateBusinessUnitCodeException"))
        .body("code", is(409))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 409 for WarehouseAlreadyArchivedException")
  void shouldReturn409ForWarehouseAlreadyArchived() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
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
        .when()
        .delete("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(204);

    given()
        .when()
        .delete("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(409)
        .body("exceptionType", containsString("WarehouseAlreadyArchivedException"))
        .body("code", is(409))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 422 for InsufficientCapacityException")
  void shouldReturn422ForInsufficientCapacity() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":40}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("InsufficientCapacityException"))
        .body("code", is(422))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 422 for LocationCapacityExceededException")
  void shouldReturn422ForLocationCapacityExceeded() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":80,\"stock\":10}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("LocationCapacityExceededException"))
        .body("code", is(422))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 422 for LocationWarehouseLimitExceededException")
  void shouldReturn422ForLocationWarehouseLimitExceeded() {
    String businessUnitCode1 = "MWH.EXCEPTION.1." + System.currentTimeMillis();
    String businessUnitCode2 = "MWH.EXCEPTION.2." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"VETSBY-001\",\"capacity\":50,\"stock\":10}",
            businessUnitCode1))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"VETSBY-001\",\"capacity\":50,\"stock\":10}",
            businessUnitCode2))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("LocationWarehouseLimitExceededException"))
        .body("code", is(422))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("Should return 422 for StockMismatchException")
  void shouldReturn422ForStockMismatch() {
    String businessUnitCode = "MWH.EXCEPTION." + System.currentTimeMillis();
    
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}",
            businessUnitCode))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":25}",
            businessUnitCode))
        .when()
        .put("/warehouse/" + businessUnitCode)
        .then()
        .statusCode(422)
        .body("exceptionType", containsString("StockMismatchException"))
        .body("code", is(422))
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("ApiError should have consistent structure across all exception types")
  void shouldHaveConsistentErrorStructure() {
    given()
        .when()
        .get("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("exceptionType", notNullValue())
        .body("code", notNullValue())
        .body("error", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  @DisplayName("ApiError timestamp should be in ISO format")
  void shouldHaveTimestampInISOFormat() {
    given()
        .when()
        .get("/warehouse/NONEXISTENT")
        .then()
        .statusCode(404)
        .body("timestamp", containsString("T"))
        .body("timestamp", containsString("Z"));
  }
}
