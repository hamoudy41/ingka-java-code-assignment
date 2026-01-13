package com.fulfilment.application.monolith.location;

/**
 * Location model representing a location with its operational constraints.
 *
 * @param identification unique location identifier
 * @param maxNumberOfWarehouses maximum number of warehouses allowed at this location
 * @param maxCapacity maximum capacity per warehouse at this location
 */
public record Location(
    String identification,
    int maxNumberOfWarehouses,
    int maxCapacity
) {}

