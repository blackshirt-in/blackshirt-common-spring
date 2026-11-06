# Blackshirt Common Spring

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot 4.0.0](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/Version-0.1.4-blue.svg)](pom.xml)
[![Package](https://img.shields.io/badge/GitHub%20Packages-in.blackshirt-blueviolet.svg)](https://github.com/blackshirt-in/blackshirt-common-spring/packages)

A standardized, production-grade Spring Boot 4 / Java 21 foundation library for the **Blackshirt microservices ecosystem**. It provides sealed API response envelopes, centralized tiered exception handling, automated perimeter request/response logging, sensitive PII payload masking, multi-tenant isolation, and distributed tracing correlation with zero boilerplate.

---

## Documentation Index

Explore detailed design, usage examples, and deep dives for each core topic:

| Topic | Document | Description |
| :--- | :--- | :--- |
| **API Responses** | [docs/api-responses.md](docs/api-responses.md) | Sealed records (`BaseResponse`, `ApiSuccessResponse`, `ApiErrorResponse`), `ResponseFactory` usage, and controller integration. |
| **Exception Handling** | [docs/exception-handling.md](docs/exception-handling.md) | Centralized 5-tier ordered exception hierarchy, `{PREFIX}_{NNN}` error code contract, and zero-pollution logging. |
| **Perimeter Logging** | [docs/logging.md](docs/logging.md) | `RequestResponseLoggingFilter`, `HttpLoggingUtils` zero-allocation processing, MDC binding, and standard logback patterns. |
| **Data Masking** | [docs/masking.md](docs/masking.md) | Single-pass consolidated regex engine, PII header/payload sanitization, and programmatic `MaskingCustomizer`. |
| **Tenant Context** | [docs/tenant-context.md](docs/tenant-context.md) | Multi-tenant isolation POJO, thread-local holder, perimeter filter, downstream HTTP interceptor, and async propagation. |
| **Distributed Tracing** | [docs/distributed-tracing.md](docs/distributed-tracing.md) | W3C `traceparent` parsing, Micrometer Tracing integration, `X-Trace-Id` response headers, and APM correlation. |
| **CI/CD & Workflows** | [docs/ci-cd-workflows.md](docs/ci-cd-workflows.md) | GitHub Actions dual-environment deployment (Stage SNAPSHOT vs Prod Release) and automated package retention cleanup. |
| **AI Agent Skill** | [.github/skills/blackshirt-common-spring/SKILL.md](.github/skills/blackshirt-common-spring/SKILL.md) | LLM prompt and engineering rules with strict **Dos and Don'ts** for consuming and maintaining the library. |

---

## Project Structure

```text
blackshirt-common-spring/
├── .github/
│   ├── skills/
│   │   └── blackshirt-common-spring/
│   │       └── SKILL.md                   # AI Agent guidance, rules, and Dos/Don'ts
│   └── workflows/
│       ├── cleanup-packages.yml           # Automated package retention (keeps 4 prod, 2 snapshots)
│       ├── publish-prod.yml               # Production release publish workflow (main branch)
│       └── publish-stage.yml              # Staging SNAPSHOT publish workflow (stage branch)
├── docs/
│   ├── api-responses.md                   # Sealed response models & ResponseFactory guide
│   ├── ci-cd-workflows.md                 # CI/CD pipelines & GitHub Packages setup
│   ├── distributed-tracing.md             # W3C traceparent & Micrometer Tracing guide
│   ├── exception-handling.md              # 5-tier ordered exception handling architecture
│   ├── logging.md                         # Boundary filter & zero-duplicate logging guide
│   ├── masking.md                         # Sensitive data & PII masking engine
│   └── tenant-context.md                  # Multi-tenant isolation & propagation guide
├── src/
│   ├── main/
│   │   ├── java/in/blackshirt/common/
│   │   │   ├── config/
│   │   │   │   ├── BlackshirtAutoConfiguration.java   # Spring Boot auto-configuration entrypoint
│   │   │   │   └── BlackshirtProperties.java          # Configuration properties (prefix: blackshirt)
│   │   │   ├── exception/
│   │   │   │   ├── code/                              # Error code contracts & standard enums
│   │   │   │   │   ├── DatabaseErrorCode.java         # ERR-DB-xxx codes
│   │   │   │   │   ├── ErrorCode.java                 # Base interface: {PREFIX}_{NNN} convention
│   │   │   │   │   ├── HttpErrorCode.java             # ERR-HTTP-xxx codes
│   │   │   │   │   ├── RuntimeErrorCode.java          # ERR-RT-xxx codes
│   │   │   │   │   └── SystemErrorCode.java           # ERR-SYS-xxx codes
│   │   │   │   ├── handler/                           # Tiered @RestControllerAdvice handlers
│   │   │   │   │   ├── AbstractExceptionHandler.java  # Common base handler & response converter
│   │   │   │   │   ├── BusinessExceptionHandler.java  # Catches BaseException (Order: HIGHEST + 300)
│   │   │   │   │   ├── DatabaseExceptionHandler.java  # Catches DataAccessException (Order: HIGHEST + 200)
│   │   │   │   │   ├── ExceptionHandlerOrder.java     # Explicit precedence constants
│   │   │   │   │   ├── FallbackExceptionHandler.java  # Catches Throwable (Order: LOWEST)
│   │   │   │   │   ├── HttpExceptionHandler.java      # Catches Spring Web MVC (Order: HIGHEST + 100)
│   │   │   │   │   └── JavaRuntimeExceptionHandler.java # Catches NPE, IllegalArgument (Order: HIGHEST + 400)
│   │   │   │   ├── BaseException.java                 # Standard business exception
│   │   │   │   ├── ErrorDetailKeys.java               # Standard diagnostic keys
│   │   │   │   ├── ErrorResponseConverter.java        # Converter abstraction
│   │   │   │   └── InternalServerException.java       # Unchecked 500 exception wrapper
│   │   │   ├── logging/
│   │   │   │   ├── HttpLoggingUtils.java              # Zero-allocation header streaming & IP extraction
│   │   │   │   ├── JsonPayloadMasker.java             # Single-pass regex masking engine
│   │   │   │   ├── MaskingCustomizer.java             # Functional interface for custom rules
│   │   │   │   ├── PayloadMasker.java                 # Pluggable masking engine interface
│   │   │   │   ├── RequestResponseLoggingFilter.java  # Perimeter servlet filter (Order: HIGHEST + 10)
│   │   │   │   └── TraceContext.java                  # Micrometer tracer bridge utility
│   │   │   ├── response/
│   │   │   │   ├── ApiErrorResponse.java              # Sealed error envelope record
│   │   │   │   ├── ApiSuccessResponse.java            # Sealed success envelope record
│   │   │   │   ├── BaseResponse.java                  # Sealed root interface
│   │   │   │   └── ResponseFactory.java               # Injectable response builder bean
│   │   │   └── tenant/
│   │   │       ├── MissingTenantException.java        # Thrown when tenant header is missing
│   │   │       ├── TenantClientRequestInterceptor.java # Downstream RestClient / RestTemplate propagation
│   │   │       ├── TenantContext.java                 # Multi-tenant coordinate POJO
│   │   │       ├── TenantContextFilter.java           # Perimeter servlet filter (Order: HIGHEST + 5)
│   │   │       ├── TenantContextHolder.java           # ThreadLocal storage utility
│   │   │       └── TenantContextProvider.java         # Spring bean context manager & MDC binder
│   │   └── resources/
│   │       └── META-INF/spring/
│   │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test/
│       └── java/in/blackshirt/common/                 # 100% passing unit & integration test suite (64 tests)
├── pom.xml                                            # Maven configuration & GitHub Packages distribution
└── README.md                                          # Project documentation entry point
```

---

## Installation & Dependency Setup

The package is hosted on **GitHub Packages**. To consume `blackshirt-common-spring` in your microservice:

### Current Version: `0.1.4`

### 1. Maven Configuration

Add the dependency to your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>in.blackshirt</groupId>
    <artifactId>blackshirt-common-spring</artifactId>
    <version>0.1.4</version>
</dependency>
```

#### GitHub Packages Repository in `pom.xml`:
```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/blackshirt-in/blackshirt-common-spring</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

#### Authentication in `~/.m2/settings.xml`:
To authenticate against GitHub Packages, configure your `settings.xml` with a GitHub Personal Access Token (PAT) having `read:packages` scope:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

---

### 2. Gradle Configuration

#### Groovy DSL (`build.gradle`):
```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/blackshirt-in/blackshirt-common-spring")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'in.blackshirt:blackshirt-common-spring:0.1.4'
}
```

#### Kotlin DSL (`build.gradle.kts`):
```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/blackshirt-in/blackshirt-common-spring")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("in.blackshirt:blackshirt-common-spring:0.1.4")
}
```

---

## Quick Start Configuration (`application.yml`)

Add standard settings to your microservice's `application.yml`:

```yaml
blackshirt:
  log:
    # Microservice identifier attached to response envelopes and MDC
    serviceName: order-service

  logging:
    enabled: true
    include-headers: false
    include-payload: false
    max-payload-length: 4096
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /favicon.ico
      - /error
    masking:
      enabled: true
      mask-replacement: "***"
      additional-masked-fields:
        - otpCode
        - panNumber

  tenant:
    enabled: true
    require-tenant-id: false
    propagate-to-mdc: true

logging:
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%15.15t]){faint} %clr([%X{traceId:-},%X{spanId:-}]){cyan} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n"
```

---

## Core Usage Examples

### 1. Controllers Returning Standard Envelopes

Inject `ResponseFactory` to build standardized envelopes:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final ResponseFactory responseFactory;

    public OrderController(OrderService orderService, ResponseFactory responseFactory) {
        this.orderService = orderService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<OrderDto>> getOrder(@PathVariable String id) {
        return responseFactory.success(orderService.findById(id), "Order retrieved");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<OrderDto>> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        OrderDto order = orderService.create(req);
        return responseFactory.created(order, "/api/v1/orders/" + order.id());
    }
}
```

### 2. Throwing Domain Exceptions

Throw `BaseException` using domain enums implementing `ErrorCode`:

```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORD_001", "Order was not found", 404),
    INSUFFICIENT_STOCK("ORD_002", "Inventory stock insufficient", 400);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    OrderErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override public String getCode() { return code; }
    @Override public String getDefaultMessage() { return defaultMessage; }
    @Override public int getHttpStatus() { return httpStatus; }
}

// In your service:
throw new BaseException(OrderErrorCode.ORDER_NOT_FOUND, "Order " + orderId + " not found");
```

### 3. Multi-Tenant Context Access

Read tenant coordinates anywhere in service code:

```java
String currentTenant = TenantContextHolder.getTenantId();
Optional<String> currentUser = TenantContextHolder.getOptionalUserId();
```

---

## Building & Verification

To run all unit tests locally:

```bash
mvn test
```

To build and install the JAR to your local `.m2` repository:

```bash
mvn clean install
```

---

## License

Copyright © 2026 Blackshirt Organization. All rights reserved.
