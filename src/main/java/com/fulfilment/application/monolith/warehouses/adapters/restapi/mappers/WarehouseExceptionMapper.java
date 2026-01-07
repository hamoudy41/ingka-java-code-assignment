package com.fulfilment.application.monolith.warehouses.adapters.restapi.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseRequestException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationWarehouseLimitExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.StockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.jbosslog.JBossLog;

@Provider
@Priority(Priorities.USER + 500)
@JBossLog
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseDomainException> {

  @Inject ObjectMapper objectMapper;

  private record ExceptionMapping(
      Predicate<WarehouseDomainException> matcher,
      Function<WarehouseDomainException, Integer> statusCodeProvider,
      Function<WarehouseDomainException, String> messageProvider) {
  }

  private static final int NOT_FOUND = 404;
  private static final int CONFLICT = 409;
  private static final int UNPROCESSABLE_ENTITY = 422;
  private static final int INTERNAL_SERVER_ERROR = 500;

  private static final ExceptionMapping[] EXCEPTION_MAPPINGS = {
      new ExceptionMapping(
          WarehouseNotFoundException.class::isInstance,
          e -> NOT_FOUND,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          DuplicateBusinessUnitCodeException.class::isInstance,
          e -> CONFLICT,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          InvalidWarehouseRequestException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          LocationCapacityExceededException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          LocationWarehouseLimitExceededException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          InsufficientCapacityException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          StockMismatchException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
      new ExceptionMapping(
          WarehouseAlreadyArchivedException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          WarehouseDomainException::getMessage),
  };

  @Override
  public Response toResponse(WarehouseDomainException exception) {
    ExceptionMapping mapping = findMapping(exception);
    if (mapping == null) {
      mapping = DEFAULT_MAPPING;
    }

    int statusCode = mapping.statusCodeProvider().apply(exception);
    String errorMessage = mapping.messageProvider().apply(exception);

    ObjectNode errorJson = objectMapper.createObjectNode();
    errorJson.put("exceptionType", exception.getClass().getName());
    errorJson.put("code", statusCode);
    if (errorMessage != null) {
      errorJson.put("error", errorMessage);
    }

    return Response.status(statusCode).entity(errorJson).build();
  }

  private static final ExceptionMapping DEFAULT_MAPPING =
      new ExceptionMapping(e -> true, e -> INTERNAL_SERVER_ERROR, WarehouseDomainException::getMessage);

  private ExceptionMapping findMapping(WarehouseDomainException exception) {
    return Arrays.stream(EXCEPTION_MAPPINGS)
        .filter(mapping -> mapping.matcher().test(exception))
        .findFirst()
        .orElse(null);
  }
}

