package com.fulfilment.application.monolith.common;

import com.fulfilment.application.monolith.location.InvalidLocationIdentifierException;
import com.fulfilment.application.monolith.location.LocationNotFoundException;
import com.fulfilment.application.monolith.stores.domain.exceptions.InvalidStoreRequestException;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import com.fulfilment.application.monolith.stores.domain.exceptions.StoreNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.StockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.stream.Collectors;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Global exception handler using Quarkus RESTEasy Reactive's ServerExceptionMapper.
 * Maps all application exceptions to appropriate HTTP responses
 */
@JBossLog
public class GlobalExceptionMapper {

  @ServerExceptionMapper
  public Response handleValidation(ResteasyReactiveViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    
    var violations = ex.getConstraintViolations().stream()
        .map(v -> new ApiError.Violation(
            v.getPropertyPath().toString(),
            v.getMessage(),
            String.valueOf(v.getInvalidValue())
        ))
        .collect(Collectors.toList());
    
    log.warnf("Validation failed: %s", message);
    
    return Response
        .status(400)
        .entity(ApiError.withViolations(
            "jakarta.validation.ConstraintViolationException",
            400,
            message,
            violations
        ))
        .build();
  }

  @ServerExceptionMapper
  public Response handleInvalidLocationIdentifier(InvalidLocationIdentifierException ex) {
    log.warnf("Invalid location identifier: %s", ex.getMessage());
    return buildResponse(ex, 400);
  }

  @ServerExceptionMapper
  public Response handleWarehouseNotFound(WarehouseNotFoundException ex) {
    log.warnf("Warehouse not found: %s", ex.getMessage());
    return buildResponse(ex, 404);
  }

  @ServerExceptionMapper
  public Response handleStoreNotFound(StoreNotFoundException ex) {
    log.warnf("Store not found: %s", ex.getMessage());
    return buildResponse(ex, 404);
  }

  @ServerExceptionMapper
  public Response handleLocationNotFound(LocationNotFoundException ex) {
    log.warnf("Location not found: %s", ex.getMessage());
    return buildResponse(ex, 404);
  }

  @ServerExceptionMapper
  public Response handleDuplicateBusinessUnitCode(DuplicateBusinessUnitCodeException ex) {
    log.warnf("Duplicate business unit code: %s", ex.getMessage());
    return buildResponse(ex, 409);
  }

  @ServerExceptionMapper
  public Response handleWarehouseAlreadyArchived(WarehouseAlreadyArchivedException ex) {
    log.warnf("Warehouse already archived: %s", ex.getMessage());
    return buildResponse(ex, 409);
  }

  @ServerExceptionMapper
  public Response handleOptimisticLock(OptimisticLockException ex) {
    log.warnf("Optimistic lock exception");
    return buildResponse(ex, 409, "The resource was modified by another transaction. Please retry.");
  }

  @ServerExceptionMapper
  public Response handleInsufficientCapacity(InsufficientCapacityException ex) {
    log.warnf("Insufficient capacity: %s", ex.getMessage());
    return buildResponse(ex, 422);
  }

  @ServerExceptionMapper
  public Response handleLocationCapacityExceeded(LocationCapacityExceededException ex) {
    log.warnf("Location capacity exceeded: %s", ex.getMessage());
    return buildResponse(ex, 422);
  }

  @ServerExceptionMapper
  public Response handleLocationWarehouseLimitExceeded(LocationWarehouseLimitExceededException ex) {
    log.warnf("Location warehouse limit exceeded: %s", ex.getMessage());
    return buildResponse(ex, 422);
  }

  @ServerExceptionMapper
  public Response handleStockMismatch(StockMismatchException ex) {
    log.warnf("Stock mismatch: %s", ex.getMessage());
    return buildResponse(ex, 422);
  }

  @ServerExceptionMapper
  public Response handleInvalidStoreRequest(InvalidStoreRequestException ex) {
    log.warnf("Invalid store request: %s", ex.getMessage());
    return buildResponse(ex, 422);
  }

  @ServerExceptionMapper
  public Response handleLegacySync(LegacySyncException ex) {
    log.errorf(ex, "Legacy sync failed");
    return buildResponse(ex, 500);
  }

  @ServerExceptionMapper
  public Response handleWebApplication(WebApplicationException ex) {
    int status = ex.getResponse().getStatus();
    log.debugf("WebApplicationException: %d", status);
    return buildResponse(ex, status);
  }

  @ServerExceptionMapper
  public Response handleUnexpected(Throwable ex) {
    log.errorf(ex, "Unexpected error");
    return buildResponse(ex, 500);
  }

  private Response buildResponse(Throwable ex, int status) {
    return buildResponse(ex, status, ex.getMessage());
  }

  private Response buildResponse(Throwable ex, int status, String message) {
    return Response
        .status(status)
        .entity(ApiError.of(ex.getClass().getName(), status, message))
        .build();
  }
}
