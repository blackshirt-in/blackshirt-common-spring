# CMS Common Library

A shared library for the Blackshirt CMS project, providing common utilities, configurations, and core features used across microservices.

## Features

- **Centralized Logging**: 
    - `LoggingAspect`: Automatically logs method execution time, arguments, and results for methods annotated with `@LogActivity`.
    - **MDC Trace Filtering**: Automatically assigns a unique `traceId` to every web request for consistent log tracing across services.
- **Global Error Handling**: Standardized exception handling via `GlobalExceptionHandler`.
- **API Response Wrapper**: A consistent `ApiResponse` structure for all REST endpoints.

## Prerequisites

- Java 17
- Maven 3.8+

## Building the Project

To build the library and install it to your local Maven repository:

```bash
mvn clean install
```

## Usage

Include the library as a dependency in your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.blackshirt.cms</groupId>
    <artifactId>cms-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The library uses Spring Boot's Auto-configuration. Ensure that the package `com.blackshirt.cms.common` (or its child packages) are scanned or that the auto-configuration is picked up via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## CI/CD

The project includes a GitHub Action workflow to automatically publish the library to GitHub Packages when changes are pushed to `dev` or `main` branches. It also runs a build verification for Pull Requests targeting these branches.

