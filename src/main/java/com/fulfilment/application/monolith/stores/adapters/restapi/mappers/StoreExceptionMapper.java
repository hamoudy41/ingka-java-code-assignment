package com.fulfilment.application.monolith.stores.adapters.restapi.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.stores.domain.exceptions.InvalidStoreRequestException;
import com.fulfilment.application.monolith.stores.domain.exceptions.LegacySyncException;
import com.fulfilment.application.monolith.stores.domain.exceptions.StoreNotFoundException;
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
@Priority(Priorities.USER + 1000)
@JBossLog
public class StoreExceptionMapper implements ExceptionMapper<RuntimeException> {

  @Inject ObjectMapper objectMapper;

  private record ExceptionMapping(
      Predicate<RuntimeException> matcher,
      Function<RuntimeException, Integer> statusCodeProvider,
      Function<RuntimeException, String> messageProvider) {
  }

  private static final int NOT_FOUND = 404;
  private static final int UNPROCESSABLE_ENTITY = 422;
  private static final int CONFLICT = 409;
  private static final int INTERNAL_SERVER_ERROR = 500;
  private static final String OPTIMISTIC_LOCK_MESSAGE =
      "The store was modified by another transaction. Please retry.";

  private static final ExceptionMapping[] EXCEPTION_MAPPINGS = {
      new ExceptionMapping(
          StoreNotFoundException.class::isInstance,
          e -> NOT_FOUND,
          RuntimeException::getMessage),
      new ExceptionMapping(
          InvalidStoreRequestException.class::isInstance,
          e -> UNPROCESSABLE_ENTITY,
          RuntimeException::getMessage),
      new ExceptionMapping(
          jakarta.persistence.OptimisticLockException.class::isInstance,
          e -> CONFLICT,
          e -> OPTIMISTIC_LOCK_MESSAGE),
      new ExceptionMapping(
          LegacySyncException.class::isInstance,
          e -> INTERNAL_SERVER_ERROR,
          e -> "Store operation succeeded but legacy synchronization failed"),
  };

  @Override
  public Response toResponse(RuntimeException exception) {
    log.errorf("Handling exception: %s - %s", exception.getClass().getSimpleName(),
        exception.getMessage(), exception);

    ExceptionMapping mapping = findMapping(exception);
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
      new ExceptionMapping(e -> true, e -> INTERNAL_SERVER_ERROR, RuntimeException::getMessage);

  private ExceptionMapping findMapping(RuntimeException exception) {
    return Arrays.stream(EXCEPTION_MAPPINGS)
        .filter(mapping -> mapping.matcher().test(exception))
        .findFirst()
        .orElse(DEFAULT_MAPPING);
  }
}

