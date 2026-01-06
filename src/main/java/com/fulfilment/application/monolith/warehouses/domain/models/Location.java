package com.fulfilment.application.monolith.warehouses.domain.models;

public record Location(
    String identification,
    int maxNumberOfWarehouses,
    int maxCapacity
) {}
