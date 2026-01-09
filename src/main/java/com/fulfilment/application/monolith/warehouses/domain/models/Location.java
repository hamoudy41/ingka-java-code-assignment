package com.fulfilment.application.monolith.warehouses.domain.models;

/**
 * Domain record representing a location with its constraints.
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
