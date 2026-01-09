package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.common.ApiError;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.adapters.restapi.dto.CreateWarehouseRequest;
import com.fulfilment.application.monolith.warehouses.adapters.restapi.dto.ReplaceWarehouseRequest;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST API resource for warehouse operations.
 * Provides endpoints for creating, retrieving, replacing, and archiving warehouses.
 */
@Path("/warehouse")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@JBossLog
@Tag(name = "Warehouses", description = "Warehouse management operations")
public class WarehouseResource {

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Inject
  ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @Inject
  ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Inject
  WarehouseStore warehouseStore;

  @Inject
  WarehouseRepository warehouseRepository;

  @GET
  @Operation(summary = "List all warehouses", description = "Retrieves a list of all warehouses in the system")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successful operation",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public List<Warehouse> listAllWarehousesUnits() {
    log.debug("Listing all warehouses");
    return warehouseRepository.listAll()
        .stream()
        .map(this::toDomainWarehouse)
        .collect(Collectors.toList());
  }

  @POST
  @Transactional
  @Operation(summary = "Create a new warehouse", description = "Creates a new warehouse with the provided details")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Warehouse created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "400", description = "Invalid request data (validation failed)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "404", description = "Location not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "409", description = "Business unit code already exists",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "422", description = "Business logic validation failed (capacity exceeded, limit reached, stock exceeds capacity)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Response createANewWarehouseUnit(@NotNull(message = "Request cannot be null.") @Valid CreateWarehouseRequest request) {
    log.debugf("Creating warehouse with business unit code '%s'", request.businessUnitCode());
    
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(request.businessUnitCode());
    warehouse.setLocation(request.location());
    warehouse.setCapacity(request.capacity());
    warehouse.setStock(request.stock());
    
    createWarehouseUseCase.create(warehouse);
    
    Warehouse created = warehouseStore.findByBusinessUnitCode(warehouse.getBusinessUnitCode());
    log.infof("Created warehouse: %s (business unit code: %s)", created.getLocation(), created.getBusinessUnitCode());
    
    return Response.ok(created).status(Response.Status.CREATED).build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Get warehouse by business unit code", description = "Retrieves a specific warehouse by its business unit code")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Warehouse found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "404", description = "Warehouse not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Warehouse getAWarehouseUnitByID(
      @Parameter(description = "Business unit code of the warehouse", required = true)
      @PathParam("id") String id) {
    log.debugf("Getting warehouse with id '%s'", id);
    
    Warehouse warehouse = warehouseStore.findByBusinessUnitCode(id);
    if (warehouse == null) {
      log.warnf("Warehouse not found with id '%s'", id);
      throw new WarehouseNotFoundException(id);
    }
    
    return warehouse;
  }

  @PUT
  @Path("/{id}")
  @Transactional
  @Operation(summary = "Replace warehouse", description = "Replaces an existing warehouse with new data")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Warehouse replaced successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "400", description = "Invalid request data (validation failed)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "404", description = "Warehouse or location not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "409", description = "Optimistic locking conflict",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "422", description = "Business logic validation failed (stock mismatch, insufficient capacity, capacity exceeded)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Warehouse replaceWarehouseUnit(
      @Parameter(description = "Business unit code of the warehouse", required = true)
      @PathParam("id") String id, 
      @NotNull(message = "Request cannot be null.") @Valid ReplaceWarehouseRequest request) {
    log.debugf("Replacing warehouse with id '%s'", id);
    
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(id);
    warehouse.setLocation(request.location());
    warehouse.setCapacity(request.capacity());
    warehouse.setStock(request.stock());
    
    replaceWarehouseUseCase.replace(warehouse);
    
    Warehouse replaced = warehouseStore.findByBusinessUnitCode(id);
    log.infof("Replaced warehouse: %s (business unit code: %s)", replaced.getLocation(), replaced.getBusinessUnitCode());
    
    return replaced;
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  @Operation(summary = "Archive warehouse", description = "Archives a warehouse by setting its archived timestamp")
  @APIResponses(value = {
      @APIResponse(responseCode = "204", description = "Warehouse archived successfully"),
      @APIResponse(responseCode = "404", description = "Warehouse not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "409", description = "Warehouse already archived",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Response archiveAWarehouseUnitByID(
      @Parameter(description = "Business unit code of the warehouse", required = true)
      @PathParam("id") String id) {
    log.debugf("Archiving warehouse with id '%s'", id);
    
    Warehouse warehouse = warehouseStore.findByBusinessUnitCode(id);
    if (warehouse == null) {
      log.warnf("Warehouse not found for archiving with id '%s'", id);
      throw new WarehouseNotFoundException(id);
    }
    
    archiveWarehouseUseCase.archive(warehouse);
    log.infof("Archived warehouse with business unit code '%s'", id);
    
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  private Warehouse toDomainWarehouse(DbWarehouse dbWarehouse) {
    Warehouse warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(dbWarehouse.getBusinessUnitCode());
    warehouse.setLocation(dbWarehouse.getLocation());
    warehouse.setCapacity(dbWarehouse.getCapacity());
    warehouse.setStock(dbWarehouse.getStock());
    if (dbWarehouse.getCreatedAt() != null) {
      warehouse.setCreationAt(dbWarehouse.getCreatedAt().atZone(ZoneId.of("UTC")));
    }
    if (dbWarehouse.getArchivedAt() != null) {
      warehouse.setArchivedAt(dbWarehouse.getArchivedAt().atZone(ZoneId.of("UTC")));
    }
    return warehouse;
  }
}
