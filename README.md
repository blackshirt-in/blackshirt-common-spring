# Blackshirt Common Spring

A standardized, production-grade Spring Boot utility library for the Blackshirt microservices ecosystem. It provides unified API response envelopes, centralized exception handling, automated perimeter request/response logging, sensitive payload masking, and distributed tracing correlation with zero boilerplate.

---

## Features

- **Sealed API Response Envelopes**: Immutable record models (`BaseResponse<T>`, `ApiSuccessResponse<T>`, `ApiErrorResponse<T>`) guaranteeing consistent API responses across all microservices.
- **`ResponseFactory`**: An injectable Spring bean that abstracts infrastructure concerns (tracing IDs, service name, timestamps, paths) and provides clean controller builder methods.
- **Extensible Error Code Convention**: An `ErrorCode` contract following the platform convention (`{SERVICE_PREFIX}_{NNN}`) alongside `StandardErrorCode` for standard HTTP errors.
- **Centralized Exception Handling**: Out-of-the-box `@RestControllerAdvice` (`GlobalExceptionHandler`) running at `@Order(LOWEST_PRECEDENCE)` that captures business and unexpected errors without stack trace pollution on 4xx rejections.
- **Perimeter HTTP Logging Filter**: `RequestResponseLoggingFilter` traces every request and response at the servlet boundary, measuring execution time (`ms`) and injecting `traceId` / `spanId` into SLF4J's `MDC`.
- **Zero Repeat Logging**: Eliminates duplicate log lines. Entry and exit are logged cleanly once, and client code never needs to catch-and-log before rethrowing.
- **Property-Driven & Code-Extensible Masking**: Sanitizes sensitive JSON fields (`password`, `token`, `secret`, `cvv`, etc.) and HTTP headers (`Authorization`, `Cookie`, etc.). Client microservices can append domain-specific fields in `application.yml` or via a programmatic `@Bean MaskingCustomizer`.
- **Distributed Tracing Integration**: `TraceContext` seamlessly bridges Micrometer Tracing (OpenTelemetry or Brave) to extract current span identifiers.
- **Zero-Boilerplate Auto-Configuration**: Automatically registers all filters, maskers, and handlers via Spring Boot's `AutoConfiguration.imports`.

---

## Prerequisites

- **Java**: 21+
- **Spring Boot**: 4.0.0+ (or 3.x compatible)
- **Maven**: 3.8+

---

## Installation

Add the dependency to your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>in.blackshirt</groupId>
    <artifactId>blackshirt-common-spring</artifactId>
    <version>0.1.3</version>
</dependency>
```

> [!NOTE]
> All core components are auto-configured via Spring Boot. No `@ComponentScan` or `@Enable...` annotations are required in your application.

---

## Configuration Reference (`application.yml`)

Configure the library in your microservice's `application.yml`:

```yaml
blackshirt:
  log:
    # Service name included in all ApiSuccessResponse and ApiErrorResponse envelopes
    serviceName: notification-service

  logging:
    # Enable or disable the perimeter RequestResponseLoggingFilter (default: true)
    enabled: true
    # Include sanitized HTTP headers in incoming request logs (default: false)
    include-headers: false
    # Include masked request and response bodies in logs (default: false)
    include-payload: false
    # Maximum payload characters to cache/log before truncating (default: 4096)
    max-payload-length: 4096
    # Paths excluded from logging and tracing filters
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /favicon.ico
      - /error

    # Sensitive data masking engine
    masking:
      # Enable or disable payload/header masking (default: true)
      enabled: true
      # Replacement string for masked values (default: "***")
      mask-replacement: "***"
      # Append service-specific fields without overwriting default base fields:
      additional-masked-fields:
        - recipientEmail
        - recipientPhone
        - otpCode
        - webhookSecret
        - apiKey
      # Append service-specific headers:
      additional-masked-headers:
        - x-custom-api-key
        - x-provider-token
```

---

## Usage Guide

### 1. Centralized Responses with `ResponseFactory`

Inject `ResponseFactory` into your controllers to construct standardized responses:

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
        OrderDto order = orderService.findById(id);
        return responseFactory.success(order, "Order retrieved successfully");
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<OrderDto>> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.create(request);
        return responseFactory.created(order, "/api/v1/orders/" + order.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteOrder(@PathVariable String id) {
        orderService.delete(id);
        return responseFactory.noContent();
    }
}
```

#### JSON Success Response Format:
```json
{
  "serviceName": "order-service",
  "status": "SUCCESS",
  "message": "Order retrieved successfully",
  "data": {
    "id": "ord_1024",
    "amount": 250.00
  },
  "timestamp": "2026-10-18T12:00:00.123Z",
  "traceId": "9f8a2b3c4d5e6f7a",
  "spanId": "b1c2d3e4f5a6b7c8",
  "path": "/api/v1/orders/ord_1024"
}
```

---

### 2. Domain Exceptions & Error Codes

#### Define Domain Error Codes
Implement the `ErrorCode` interface using an enum following the `{PREFIX}_{NNN}` convention:

```java
public enum OrderErrorCode implements ErrorCode {

    INSUFFICIENT_STOCK("ORD_001", "Insufficient inventory stock for requested items", 400),
    ORDER_NOT_FOUND("ORD_002", "Requested order could not be found", 404),
    PAYMENT_FAILED("ORD_003", "Payment processing failed", 402),
    ORDER_EXPIRED("ORD_004", "Order has expired and can no longer be processed", 410);

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

#### Throw Domain Exceptions Extending `BaseException`
```java
@Service
public class OrderService {

    public OrderDto findById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BaseException(OrderErrorCode.ORDER_NOT_FOUND, "Order " + id + " not found"));
    }

    public void reserveStock(String sku, int quantity) {
        if (inventory.getStock(sku) < quantity) {
            Map<String, Object> details = Map.of("sku", sku, "available", 2, "requested", quantity);
            throw new BaseException(OrderErrorCode.INSUFFICIENT_STOCK, "Not enough items in stock", details);
        }
    }
}
```

---

### 3. Global Exception Handling

All uncaught exceptions and `BaseException`s are automatically intercepted by `GlobalExceptionHandler`:
- **4xx Client Errors**: Logged as a concise **`WARN` single-line** without noisy 50-line stack traces.
- **5xx Server Errors**: Logged as **`ERROR` with full stack trace exactly once**.

#### JSON Error Response Format:
```json
{
  "serviceName": "order-service",
  "errorCode": "ORD_001",
  "errorMessage": "Not enough items in stock",
  "details": {
    "sku": "BLK-TSHIRT-L",
    "available": 2,
    "requested": 5
  },
  "timestamp": "2026-10-18T12:05:00.450Z",
  "traceId": "9f8a2b3c4d5e6f7a",
  "spanId": "b1c2d3e4f5a6b7c8",
  "path": "/api/v1/orders"
}
```

#### Custom Service-Specific Exception Handlers (Optional)
Client microservices can register their own higher-priority handlers:

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomOrderExceptionHandler {

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handlePaymentError(PaymentGatewayException ex) {
        // Runs BEFORE GlobalExceptionHandler
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.error("order-service", OrderErrorCode.PAYMENT_FAILED, ex.getMessage(), null, null, null, null));
    }
}
```

---

### 4. Automated Request & Response Tracing

The perimeter filter `RequestResponseLoggingFilter` automatically manages the request lifecycle:
1. Injects `traceId`, `spanId`, and `clientIp` into SLF4J's `MDC`.
2. Adds `X-Trace-Id` to outgoing HTTP response headers.
3. Automatically logs single-line entry and exit metrics:

```text
2026-10-18 12:10:00.100 [exec-1] [traceId=a1b2c3d4,spanId=e5f6] INFO RequestResponseLoggingFilter - --> POST /api/v1/orders (client=10.0.0.5)
2026-10-18 12:10:00.125 [exec-1] [traceId=a1b2c3d4,spanId=e5f6] INFO OrderService                 - Creating order for customerId=108
2026-10-18 12:10:00.145 [exec-1] [traceId=a1b2c3d4,spanId=e5f6] INFO RequestResponseLoggingFilter - <-- 201 Created POST /api/v1/orders [45ms]
```

---

### 5. Sensitive Data Masking Configuration

#### Option A: Property-Driven (Zero Java Code)
Simply add fields to `application.yml`:
```yaml
blackshirt:
  logging:
    masking:
      additional-masked-fields:
        - rawCreditCardNumber
        - ssn
        - userPin
```

#### Option B: Programmatic Client Hook (`MaskingCustomizer`)
Register a Spring bean in your client microservice:
```java
@Configuration
public class SecurityLoggingConfig {

    @Bean
    public MaskingCustomizer customMaskingCustomizer() {
        return masking -> {
            masking.getAdditionalMaskedFields().add("vendorApiKey");
            masking.getAdditionalMaskedHeaders().add("X-Vendor-Signature");
            masking.setMaskReplacement("[PROTECTED]");
        };
    }
}
```

#### Option C: Custom Masking Algorithm (`PayloadMasker`)
Replace the default masking engine with a specialized implementation (e.g., PCI-DSS partial masking) via `@Bean`:
```java
@Configuration
public class PciLoggingConfig {

    @Bean
    public PayloadMasker payloadMasker(BlackshirtProperties properties) {
        return new CustomPciCompliantMasker(properties.getLogging().getMasking());
    }
}
```

---

### 6. Distributed Tracing (`TraceContext`)

Access active distributed tracing identifiers anywhere in your application:

```java
Tracer tracer = ...; // Injected Micrometer Tracer
TraceContext.TraceIds traceIds = TraceContext.current(tracer);

String traceId = traceIds.traceId();
String spanId = traceIds.spanId();
```

---

## Client Code Best Practices (Anti-Duplication Rules)

To maintain clean, readable production logs across all services:

1. **Only Log Domain Milestones**:
   ```java
   // ✅ GOOD:
   log.info("Processing shipment for orderId={}", orderId);

   // ❌ BAD: Redundant (filter already logs entry, exit, timing)
   log.info("Entering createOrder()");
   ```

2. **Never Catch-and-Log Before Rethrowing**:
   ```java
   // ✅ GOOD: GlobalExceptionHandler logs it once with context
   if (!isValid(request)) {
       throw new BaseException(OrderErrorCode.INVALID_REQUEST, "Invalid order");
   }

   // ❌ BAD: Results in 2 or 3 duplicate stack traces in your log aggregators!
   try {
       processOrder();
   } catch (Exception ex) {
       log.error("Order failed", ex); // DUPLICATE LOG!
       throw ex;
   }
   ```

---

## Building & Testing

To build the library, run the test suite, and install to your local Maven repository:

```bash
mvn clean install
```

To run only the unit tests:
```bash
mvn test
```

---

## License

Copyright © 2026 Blackshirt Organization. All rights reserved.
