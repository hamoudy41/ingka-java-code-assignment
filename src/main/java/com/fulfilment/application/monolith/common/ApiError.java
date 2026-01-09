package com.fulfilment.application.monolith.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error response contract for all API errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String exceptionType,
    int code,
    String error,
    Instant timestamp,
    List<Violation> violations
) {
  
  public static ApiError of(String exceptionType, int code, String error) {
    return new ApiError(exceptionType, code, error, Instant.now(), null);
  }

  public static ApiError withViolations(String exceptionType, int code, String error, List<Violation> violations) {
    return new ApiError(exceptionType, code, error, Instant.now(), violations);
  }

  public record Violation(String field, String message, String invalidValue) {}
}
