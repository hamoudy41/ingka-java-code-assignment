# Java Code Assignment

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

---

## Testing & Quality Assurance

### Running Tests

Execute the complete test suite:

```sh
./mvnw test
```

Run tests with coverage report:

```sh
./mvnw clean verify
```

The coverage report will be generated at `target/jacoco-report/index.html`.

### Test Coverage

The project uses **JaCoCo** for code coverage analysis, integrated via Quarkus. Coverage is configured to:
- Track all application code in `com/fulfilment/**`
- Exclude generated code and dependencies
- Generate HTML reports for detailed analysis
- Maintain coverage targets above 80%

Configuration is managed in `application.properties`:

```properties
%test.quarkus.jacoco.enabled=true
%test.quarkus.jacoco.report-location=target/jacoco-report
%test.quarkus.jacoco.includes=com/fulfilment/**
```

### Test Categories

**Unit & Integration Tests**
- Repository layer tests (data access, persistence)
- Use case tests (business logic, validation rules)
- Resource tests (REST API endpoints, request/response handling)
- Exception mapper tests (error handling, status codes)
- Service tests (transaction synchronization, legacy system integration)

**Edge Cases Covered**
- Input validation (null, empty, boundary values)
- Business rule violations (capacity limits, stock constraints)
- Concurrency scenarios (optimistic locking, version conflicts)
- Error conditions (not found, duplicates, conflicts)

**Test Isolation**
- Tests use `@BeforeEach` cleanup for isolation
- Each test operates independently
- Database state is reset between test runs

### API Testing

**Postman Collection**

A comprehensive Postman collection is provided in `Fulfilment_API.postman_collection.json` covering:
- All CRUD operations for Stores, Products, and Warehouses
- Success scenarios with valid data
- Error scenarios (validation failures, not found, conflicts)
- Edge cases (boundary values, business logic violations)
- Pre-configured collection variables

**To use the Postman collection:**
1. Import `Fulfilment_API.postman_collection.json` into Postman
2. Ensure the application is running (`./mvnw quarkus:dev`)
3. The base URL is pre-configured to `http://localhost:8080`
4. Run individual requests or the entire collection

**Swagger UI**

Interactive API documentation is available via Swagger UI:

```
http://localhost:8080/swagger-ui
```

Features:
- Complete OpenAPI specification for all endpoints
- Interactive "Try it out" functionality
- Detailed request/response schemas
- All possible HTTP status codes documented
- Organized by domain (Warehouses, Stores, Products)

**OpenAPI Specification**

Download the OpenAPI spec:
- JSON format: `http://localhost:8080/openapi`
- YAML format: `http://localhost:8080/openapi?format=yaml`

### Exception Handling & Status Codes

The API uses consistent HTTP status codes:
- `200 OK` - Successful GET/PUT/PATCH operations
- `201 Created` - Successful resource creation
- `204 No Content` - Successful DELETE operations
- `400 Bad Request` - Invalid input (validation failures)
- `404 Not Found` - Resource not found
- `409 Conflict` - Business conflicts (duplicates like store/product name, warehouse businessUnitCode; already archived)
- `422 Unprocessable Entity` - Business logic violations
- `500 Internal Server Error` - Unexpected server errors

All error responses follow a standardized format with `exceptionType`, `code`, `error`, and `timestamp` fields.

### API Conventions (House Style)

- **DTOs in/out**: REST resources accept request DTOs (`Create*Request`, `Update*Request`, `Patch*Request`) and return response DTOs (`*Response`). We avoid returning JPA entities or domain models directly from REST.
- **Validation**:
  - **Request/shape validation** is done with Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) on request DTOs and results in **400**.
  - **Business rule validation** is expressed via **custom exceptions per bounded context** and mapped to **409/422** (depending on conflict vs rule violation).
- **Exceptions**:
  - Avoid throwing generic exceptions (`RuntimeException`, `WebApplicationException`, `IllegalArgumentException`) for expected flow.
  - Prefer explicit, context-scoped exceptions and keep mapping centralized in `GlobalExceptionMapper`.

---

Have fun, and join the team of contributors!
