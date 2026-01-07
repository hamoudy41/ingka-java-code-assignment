# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor for consistency and clearer boundaries.

- Unify the persistence approach per bounded context. Today we mix patterns (Panache active-record style on entities, repositories, and direct EntityManager usage). That increases cognitive load and makes transaction ownership less obvious.
- Prefer a repository/port (hexagonal) style for write operations and domain use cases where business rules exist (stores/warehouses). It keeps persistence in adapters and makes the domain/use-case layer easier to test and evolve.
- Keep persistence models focused on storage concerns (mapping, optimistic locking), and move input validation to DTOs and use cases. This avoids validation leaking into domain entities and prevents surprises with partial updates.
- Standardize exception mapping per module. A broad exception mapper can accidentally intercept errors from other modules; using typed base exceptions per module keeps HTTP semantics predictable and avoids cross-module interference.
- Standardize time handling. Persist timestamps in UTC (or use an instant-capable type) and map consistently to avoid timezone drift and environment-dependent behavior.

Net result: simpler mental model, fewer hidden side effects, better test isolation, and easier long-term maintenance.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (generated code)
- Pros: explicit contract, easier alignment with external consumers, strong client/server consistency, better governance/versioning, and enables parallel work across teams.
- Cons: potential impedance mismatch with the domain model, risk of generated types leaking past the REST adapter, and regeneration noise for small changes.

Code-first (manual JAX-RS/resources)
- Pros: fastest iteration, direct control over behavior and mapping, fewer generated artifacts, and often easier to keep a clean boundary (DTOs at the edge, domain models/use cases inside).
- Cons: spec can drift from implementation unless generated/validated, and consumer tooling quality depends on how the OpenAPI is produced and maintained.

My choice:
- For consumer-facing or cross-team APIs: OpenAPI-first, but keep generated models/interfaces confined to the REST adapter and map to internal DTOs/domain models.
- For internal APIs where iteration speed matters: code-first, while generating an OpenAPI spec from annotations and enforcing it in CI.

In this repository, I would standardize on one approach per API surface and ensure any OpenAPI-generated artifacts do not shape the domain model.

```