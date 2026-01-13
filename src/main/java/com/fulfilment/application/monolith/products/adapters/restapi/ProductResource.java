package com.fulfilment.application.monolith.products.adapters.restapi;

import com.fulfilment.application.monolith.common.ApiError;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.products.adapters.restapi.dto.CreateProductRequest;
import com.fulfilment.application.monolith.products.adapters.restapi.dto.ProductResponse;
import com.fulfilment.application.monolith.products.adapters.restapi.dto.UpdateProductRequest;
import com.fulfilment.application.monolith.products.domain.exceptions.ProductAlreadyExistsException;
import com.fulfilment.application.monolith.products.domain.exceptions.ProductNotFoundException;
import io.quarkus.panache.common.Sort;
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
 * REST API resource for product operations.
 * Provides endpoints for CRUD operations on products.
 */
@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@JBossLog
@Tag(name = "Products", description = "Product management operations")
public class ProductResource {

  @Inject
  ProductRepository productRepository;

  @GET
  @Operation(summary = "List all products", description = "Retrieves a list of all products sorted by name")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successful operation",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public List<ProductResponse> get() {
    return productRepository.listAll(Sort.by("name"))
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @GET
  @Path("{id}")
  @Operation(summary = "Get product by ID", description = "Retrieves a specific product by its ID")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @APIResponse(responseCode = "404", description = "Product not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public ProductResponse getSingle(
      @Parameter(description = "ID of the product", required = true)
      @PathParam("id") Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new ProductNotFoundException(id);
    }
    return toResponse(entity);
  }

  @POST
  @Transactional
  @Operation(summary = "Create a new product", description = "Creates a new product with the provided details")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Product created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @APIResponse(responseCode = "400", description = "Invalid request data (validation failed)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Response create(@NotNull(message = "Request cannot be null.") @Valid CreateProductRequest request) {
    if (productRepository.count("name", request.name()) > 0) {
      throw new ProductAlreadyExistsException(request.name());
    }
    Product product = new Product();
    product.setName(request.name());
    product.setDescription(request.description());
    product.setPrice(request.price());
    product.setStock(request.stock());
    productRepository.persist(product);
    return Response.ok(toResponse(product)).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  @Operation(summary = "Update product", description = "Updates an existing product with new data")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @APIResponse(responseCode = "404", description = "Product not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "400", description = "Invalid request data (validation failed)",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public ProductResponse update(
      @Parameter(description = "ID of the product", required = true)
      @PathParam("id") Long id,
      @NotNull(message = "Request cannot be null.") @Valid UpdateProductRequest request) {
    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new ProductNotFoundException(id);
    }

    // Avoid an extra DB roundtrip when name is unchanged
    if (entity.getName() == null || !entity.getName().equals(request.name())) {
      if (productRepository.count("name = ?1 AND id <> ?2", request.name(), id) > 0) {
        throw new ProductAlreadyExistsException(request.name());
      }
    }

    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setPrice(request.price());
    entity.setStock(request.stock());

    productRepository.persist(entity);

    return toResponse(entity);
  }

  @DELETE
  @Path("{id}")
  @Transactional
  @Operation(summary = "Delete product", description = "Deletes a product from the system")
  @APIResponses(value = {
      @APIResponse(responseCode = "204", description = "Product deleted successfully"),
      @APIResponse(responseCode = "404", description = "Product not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Response delete(
      @Parameter(description = "ID of the product", required = true)
      @PathParam("id") Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new ProductNotFoundException(id);
    }
    productRepository.delete(entity);
    return Response.status(204).build();
  }

  private ProductResponse toResponse(Product entity) {
    return new ProductResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getPrice(),
        entity.getStock()
    );
  }
}

