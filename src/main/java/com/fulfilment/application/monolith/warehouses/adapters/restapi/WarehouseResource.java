package com.fulfilment.application.monolith.warehouses.adapters.restapi;

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

@Path("/warehouse")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@JBossLog
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
  public List<Warehouse> listAllWarehousesUnits() {
    log.debug("Listing all warehouses");
    return warehouseRepository.listAll()
        .stream()
        .map(this::toDomainWarehouse)
        .collect(Collectors.toList());
  }

  @POST
  @Transactional
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
  public Warehouse getAWarehouseUnitByID(@PathParam("id") String id) {
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
  public Warehouse replaceWarehouseUnit(@PathParam("id") String id, 
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
  public Response archiveAWarehouseUnitByID(@PathParam("id") String id) {
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
