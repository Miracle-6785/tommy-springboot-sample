# Spring Boot Template

A Spring Boot application template with SBOM generation, code coverage testing, and RESTful API examples.

## Features

- **Complete User API**: CRUD operations for a User entity
- **OpenAPI Documentation**: Interactive API documentation using Springdoc OpenAPI
- **SBOM Generation**: Software Bill of Materials generation using CycloneDX
- **Code Coverage**: Test coverage reports using JaCoCo
- **H2 Database**: In-memory database for development and testing

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.6.3 or later

### Running the Application

```bash
./mvnw spring-boot:run
```

The application will start on port 8080. You can access the API at:

- API: http://localhost:8080/api/
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

### Building the Application

```bash
./mvnw clean install
```

## SBOM Generation

This template includes CycloneDX Maven plugin for generating Software Bill of Materials (SBOM). SBOM is automatically generated during the package phase:

```bash
./mvnw package
```

The SBOM files will be generated in the `target` directory in both JSON and XML formats:

- `target/bom.json`
- `target/bom.xml`

You can manually generate the SBOM with:

```bash
./mvnw cyclonedx:makeAggregateBom
```

### SBOM Verification

To verify the SBOM file for vulnerabilities:

```bash
# Using CycloneDX CLI tool
cyclonedx-cli analyze --input target/bom.json
```

## Code Coverage

JaCoCo is configured to generate code coverage reports during the test phase:

```bash
./mvnw test
```

Coverage reports are available in:

- `target/site/jacoco/index.html`

The build will fail if line coverage drops below 70% according to the configuration.

## API Documentation

The API is documented using OpenAPI 3 with Springdoc. You can access the documentation at:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Sample User API

The template includes a complete User API with the following endpoints:

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update an existing user
- `DELETE /api/users/{id}` - Delete a user

Example user creation request:

```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

## Testing

Run tests with:

```bash
./mvnw test
```

The template includes comprehensive tests for:

- Controller layer using MockMvc
- Service layer unit tests with Mockito

## License

This template is available under the Apache License 2.0.
