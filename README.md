# Blackshirt Common Spring

A shared Spring Boot library for Blackshirt microservices, providing centralized response structures, exception handling, tracing, and auto-configuration.

## Features

- **Centralized Response Structure**: A sealed `BaseResponse<T>` hierarchy with `ApiSuccessResponse` and `ApiErrorResponse` records for consistent API responses across all services.
- **`ResponseFactory`**: A Spring bean that hides infrastructure concerns (tracing, service name, timestamps) and provides clean builder methods for controllers.
- **Global Error Handling**: Standardized exception handling via `GlobalExceptionHandler` with `@Order(LOWEST_PRECEDENCE)` — client services can register higher-priority handlers for domain-specific exceptions.
- **Extensible Error Codes**: An `ErrorCode` interface with a documented convention (`{SERVICE_PREFIX}_{NNN}`) that client services implement as enums for domain-specific errors.
- **Distributed Tracing**: `TraceContext` utility for extracting Micrometer traceId/spanId from the current span.

## Prerequisites

- Java 21
- Maven 3.8+
- Spring Boot 4.0.0

## Building the Project

To build the library and install it to your local Maven repository:

```bash
mvn clean install -T 1C -ntp
```

## Usage

### 1. Add the Dependency

Include the library in your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>in.blackshirt</groupId>
    <artifactId>blackshirt-common-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The library uses Spring Boot auto-configuration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. No package scanning configuration is needed.

### 2. Configure Properties

Add the required properties to your `application.yml`:

```yaml
blackshirt:
  log:
    serviceName: order-service
```

### 3. Use ResponseFactory in Controllers

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ResponseFactory responseFactory;

    public OrderController(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<OrderDto>> getOrder(@PathVariable String id) {
        OrderDto order = orderService.findById(id);
        return responseFactory.success(order, "Order retrieved");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<OrderDto>> create(@RequestBody CreateOrderRequest req) {
        OrderDto order = orderService.create(req);
        return responseFactory.success(order, "Order created", HttpStatus.CREATED);
    }
}
```

### 4. Define Service-Specific Error Codes

```java
public enum OrderErrorCode implements ErrorCode {

    INSUFFICIENT_STOCK("ORD_001", "Insufficient stock for the requested item", 409),
    ORDER_EXPIRED("ORD_002", "Order has expired", 410);

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
```

### 5. Add Service-Specific Exception Handlers (Optional)

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderExceptionHandler {

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiErrorResponse<?>> handle(PaymentFailedException ex) {
        // Custom handling — runs BEFORE GlobalExceptionHandler
    }
}
```

Unhandled exceptions fall through to the library's `GlobalExceptionHandler`.

## CI/CD

The project includes a GitHub Actions workflow to automatically publish the library to GitHub Packages when changes are pushed to `dev`. Pull Requests targeting `main` trigger a build verification.
