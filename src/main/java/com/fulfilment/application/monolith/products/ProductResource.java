package com.fulfilment.application.monolith.products;

import com.fulfilment.application.monolith.common.ApiError;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

  @Inject ProductRepository productRepository;

  @GET
  @Operation(summary = "List all products", description = "Retrieves a list of all products sorted by name")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successful operation",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  @Operation(summary = "Get product by ID", description = "Retrieves a specific product by its ID")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "404", description = "Product not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Product getSingle(
      @Parameter(description = "ID of the product", required = true)
      @PathParam("id") Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  @Operation(summary = "Create a new product", description = "Creates a new product with the provided details")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Product created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "422", description = "ID was invalidly set on request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Response create(Product product) {
    if (product.getId() != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  @Operation(summary = "Update product", description = "Updates an existing product with new data")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "404", description = "Product not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "422", description = "Product name was not set on request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
      @APIResponse(responseCode = "500", description = "Internal server error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
  })
  public Product update(
      @Parameter(description = "ID of the product", required = true)
      @PathParam("id") Long id, 
      Product product) {
    if (product.getName() == null) {
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    entity.setName(product.getName());
    entity.setDescription(product.getDescription());
    entity.setPrice(product.getPrice());
    entity.setStock(product.getStock());

    productRepository.persist(entity);

    return entity;
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
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    productRepository.delete(entity);
    return Response.status(204).build();
  }
}
