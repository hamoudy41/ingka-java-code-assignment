package com.fulfilment.application.monolith.warehouses.adapters.restapi.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.location.LocationNotFoundException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.jbosslog.JBossLog;

@Provider
@Priority(Priorities.USER + 500)
@JBossLog
public class WarehouseLocationNotFoundExceptionMapper implements ExceptionMapper<LocationNotFoundException> {

  @Inject ObjectMapper objectMapper;

  private static final int NOT_FOUND = 404;

  @Override
  public Response toResponse(LocationNotFoundException exception) {
    log.warnf("Location not found: %s", exception.getMessage());

    ObjectNode errorJson = objectMapper.createObjectNode();
    errorJson.put("exceptionType", exception.getClass().getName());
    errorJson.put("code", NOT_FOUND);
    if (exception.getMessage() != null) {
      errorJson.put("error", exception.getMessage());
    }

    return Response.status(NOT_FOUND).entity(errorJson).build();
  }
}


