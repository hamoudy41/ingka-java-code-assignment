package com.fulfilment.application.monolith.stores.adapters.restapi.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.stores.domain.exceptions.InvalidStoreRequestException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import lombok.extern.jbosslog.JBossLog;

@Provider
@Priority(Priorities.USER + 2000)
@JBossLog
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

  @Inject ObjectMapper objectMapper;

  private static final int UNPROCESSABLE_ENTITY = 422;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    String errorMessage = exception.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));

    log.warnf("Validation failed: %s", errorMessage);

    ObjectNode errorJson = objectMapper.createObjectNode();
    errorJson.put("exceptionType", InvalidStoreRequestException.class.getName());
    errorJson.put("code", UNPROCESSABLE_ENTITY);
    errorJson.put("error", errorMessage);

    return Response.status(UNPROCESSABLE_ENTITY).entity(errorJson).build();
  }
}

