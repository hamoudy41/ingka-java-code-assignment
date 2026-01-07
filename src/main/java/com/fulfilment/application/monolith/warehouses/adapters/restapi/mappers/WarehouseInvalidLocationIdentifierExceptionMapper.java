package com.fulfilment.application.monolith.warehouses.adapters.restapi.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.location.InvalidLocationIdentifierException;
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
public class WarehouseInvalidLocationIdentifierExceptionMapper
    implements ExceptionMapper<InvalidLocationIdentifierException> {

  @Inject ObjectMapper objectMapper;

  private static final int BAD_REQUEST = 400;

  @Override
  public Response toResponse(InvalidLocationIdentifierException exception) {
    log.warnf("Invalid location identifier: %s", exception.getMessage());

    ObjectNode errorJson = objectMapper.createObjectNode();
    errorJson.put("exceptionType", exception.getClass().getName());
    errorJson.put("code", BAD_REQUEST);
    if (exception.getMessage() != null) {
      errorJson.put("error", exception.getMessage());
    }

    return Response.status(BAD_REQUEST).entity(errorJson).build();
  }
}


