package com.fulfilment.application.monolith.stores.adapters.restapi;

import com.fulfilment.application.monolith.stores.adapters.database.Store;
import com.fulfilment.application.monolith.stores.adapters.legacy.StoreSyncService;
import com.fulfilment.application.monolith.stores.adapters.restapi.dto.CreateStoreRequest;
import com.fulfilment.application.monolith.stores.adapters.restapi.dto.PatchStoreRequest;
import com.fulfilment.application.monolith.stores.adapters.restapi.dto.UpdateStoreRequest;
import com.fulfilment.application.monolith.stores.domain.exceptions.StoreNotFoundException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import lombok.extern.jbosslog.JBossLog;

@Path("stores")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@JBossLog
public class StoreResource {

  @Inject StoreSyncService storeSyncService;

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    return findStoreOrThrow(id);
  }

  @POST
  @Transactional
  public Response create(@NotNull(message = "Store cannot be null.") @Valid CreateStoreRequest request) {
    Store store = new Store();
    store.setName(request.name());
    store.setQuantityProductsInStock(request.quantityProductsInStock());
    store.persist();
    
    log.infof("Created store: %s (id: %d)", store.getName(), store.getId());
    storeSyncService.scheduleCreateSync(store);
    return Response.ok(store).status(Response.Status.CREATED).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, @Valid UpdateStoreRequest request) {
    Store entity = findStoreOrThrow(id);
    return updateStore(entity, request, id);
  }

  private Store updateStore(Store entity, UpdateStoreRequest request, Long id) {
    Long originalVersion = entity.getVersion();
    entity.setName(request.name());
    entity.setQuantityProductsInStock(request.quantityProductsInStock());

    try {
      entity.persist();
      log.infof("Updated store: %s (id: %d, version: %d -> %d)",
          entity.getName(), entity.getId(), originalVersion, entity.getVersion());
      storeSyncService.scheduleUpdateSync(entity);
      return entity;
    } catch (OptimisticLockException e) {
      log.warnf(e, "Optimistic lock conflict for store %d (expected: %d, actual: %d)",
          id, originalVersion, entity.getVersion());
      throw e;
    }
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, @Valid PatchStoreRequest request) {
    Store entity = findStoreOrThrow(id);
    boolean updated = applyPartialUpdates(entity, request);
    return updated ? persistAndSync(entity, id) : entity;
  }

  private boolean applyPartialUpdates(Store entity, PatchStoreRequest request) {
    boolean updated = false;
    if (request.name() != null && !request.name().trim().isEmpty()) {
      entity.setName(request.name().trim());
      updated = true;
    }
    if (request.quantityProductsInStock() != null) {
      entity.setQuantityProductsInStock(request.quantityProductsInStock());
      updated = true;
    }
    return updated;
  }

  private Store persistAndSync(Store entity, Long id) {
    Long originalVersion = entity.getVersion();
    try {
      entity.persist();
      log.infof("Patched store: %s (id: %d, version: %d -> %d)",
          entity.getName(), entity.getId(), originalVersion, entity.getVersion());
      storeSyncService.scheduleUpdateSync(entity);
      return entity;
    } catch (OptimisticLockException e) {
      log.warnf(e, "Optimistic lock conflict for store %d (expected: %d, actual: %d)",
          id, originalVersion, entity.getVersion());
      throw e;
    }
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = findStoreOrThrow(id);
    entity.delete();
    log.infof("Deleted store: %s (id: %d)", entity.getName(), id);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  private Store findStoreOrThrow(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new StoreNotFoundException(id);
    }
    return entity;
  }

}

