package com.fulfilment.application.monolith.warehouses.adapters.restapi.dto;

import java.time.ZonedDateTime;

/**
 * Response DTO for warehouse operations.
 */
public record WarehouseResponse(
    String businessUnitCode,
    String location,
    Integer capacity,
    Integer stock,
    ZonedDateTime creationAt,
    ZonedDateTime archivedAt
) {}

