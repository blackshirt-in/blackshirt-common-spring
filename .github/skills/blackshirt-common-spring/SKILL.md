---
name: blackshirt-common-spring
description: Engineering standards, architectural contracts, and actionable Dos and Don'ts for AI agents and developers building microservices with or contributing to the blackshirt-common-spring library.
---

# AI Agent Skill: `blackshirt-common-spring`

This skill provides operational rules, architectural patterns, and strict **Dos and Don'ts** for AI agents generating code in Blackshirt microservices or maintaining `blackshirt-common-spring`.

---

## 1. Core Architectural Mental Model

When writing code in or for Blackshirt microservices, keep these core contracts in mind:

1. **Sealed Response Envelopes**: All public HTTP endpoints return `ResponseEntity<ApiSuccessResponse<T>>` or `ResponseEntity<ApiErrorResponse<T>>`. Never return bare entity models, raw DTOs, or naked `ResponseEntity<OrderDto>`.
2. **`ResponseFactory` Abstraction**: Always inject and use [`ResponseFactory`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/response/ResponseFactory.java) in controllers to construct envelopes. Never instantiate `ApiSuccessResponse` manually unless writing unit tests for the factory itself.
3. **`ErrorCode` Contract**: Error codes are domain enums implementing [`ErrorCode`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/exception/code/ErrorCode.java), strictly formatted as `{SERVICE_PREFIX}_{NNN}` (e.g., `ORD_001`, `PAY_042`).
4. **Tiered Exception Handlers**: Exceptions are intercepted by 5 ordered `@RestControllerAdvice` handlers. Do not catch exceptions inside controllers or services just to log them.
5. **Zero Duplicate Logging**:
   - `RequestResponseLoggingFilter` logs request entry (`-->`) and exit (`<--`) with duration and status.
   - Handlers log 4xx as concise `WARN` (no stack trace) and 5xx as `ERROR` (with stack trace exactly once).
   - Application code must **NEVER** catch-and-log before rethrowing, and must **NEVER** log controller entry/exit.
6. **Multi-Tenancy Propagation**: Access tenant coordinates via [`TenantContextHolder`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/tenant/TenantContextHolder.java). Propagate downstream using [`TenantClientRequestInterceptor`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/tenant/TenantClientRequestInterceptor.java). Always clear child threads in `finally` blocks when using async executors.

---

## 2. Comprehensive Dos and Don'ts Matrix

### 2.1 API Responses & Controller Design

| DO | DON'T |
| :--- | :--- |
| **DO** inject [`ResponseFactory`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/response/ResponseFactory.java) into `@RestController` classes. | **DON'T** manually instantiate `new ApiSuccessResponse<>(...)` inside controllers. |
| **DO** specify `ResponseEntity<ApiSuccessResponse<T>>` as the explicit controller method return type. | **DON'T** return raw POJOs/DTOs (e.g., `public OrderDto getOrder()`) or un-enveloped `ResponseEntity<T>`. |
| **DO** return `responseFactory.created(data, location)` with a URI on successful resource creations (`201 Created`). | **DON'T** return HTTP `200 OK` for resource creations where a `Location` header is expected. |
| **DO** return `responseFactory.noContent()` (`204 No Content`) for idempotent DELETE operations. | **DON'T** return a success envelope with null data and a 200 OK for resource deletions. |
| **DO** annotate request bodies with `@Valid` to trigger automatic Spring validation and `HttpExceptionHandler` formatting. | **DON'T** write manual imperative validation checks in controllers that return custom error maps. |

---

### 2.2 Exception & Error Handling

| DO | DON'T |
| :--- | :--- |
| **DO** implement [`ErrorCode`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/exception/code/ErrorCode.java) on a domain enum using the `{SERVICE_PREFIX}_{NNN}` convention (e.g. `ORD_001`). | **DON'T** use ad-hoc string literals (e.g. `"ORDER_NOT_FOUND"`, `"ERR_404"`) or bypass the `ErrorCode` contract. |
| **DO** throw [`BaseException`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/exception/BaseException.java) or a domain-specific subclass when business invariants are violated. | **DON'T** throw generic unchecked exceptions like `RuntimeException("Order not found")` for expected business rules. |
| **DO** pass structured diagnostic context via `Map<String, Object> details` using keys from [`ErrorDetailKeys`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/exception/ErrorDetailKeys.java). | **DON'T** concatenate debug data into the human-readable error message string. |
| **DO** let exceptions bubble up to the centralized tiered exception handlers (`HttpExceptionHandler`, `BusinessExceptionHandler`, etc.). | **DON'T** place broad `try { ... } catch (Exception e)` blocks in controllers or services to return custom error responses. |
| **DO** use `InternalServerException` when wrapping unexpected third-party checked exceptions. | **DON'T** swallow exceptions or return `null` when a downstream operation fails. |

---

### 2.3 Logging & Tracing

| DO | DON'T |
| :--- | :--- |
| **DO** let [`RequestResponseLoggingFilter`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/logging/RequestResponseLoggingFilter.java) log request arrival, headers, client IP, status, and duration. | **DON'T** add `log.info("Entering endpoint...")` or `log.info("Exiting endpoint...")` in controller methods. |
| **DO** log significant domain milestones in service logic (e.g., `log.info("Order {} transitioned to SHIPPED", id)`). | **DON'T** log every line or method invocation in your service code (avoid log spam). |
| **DO** let the exception handlers log exceptions. 4xx errors are logged cleanly without stack traces; 5xx errors log the stack trace once. | **DON'T** catch an exception, log it with `log.error("Failed", e)`, and then rethrow it (prevents duplicate logs). |
| **DO** use parameterized SLF4J placeholders (`log.info("Processing item: {}", item)`). | **DON'T** use string concatenation in log statements (`log.info("Processing item: " + item)`). |
| **DO** configure console patterns with `%clr([%X{traceId:-},%X{spanId:-}]){cyan}` to expose active distributed trace context. | **DON'T** attempt to manually read and print `traceId` inside application log strings. |

---

### 2.4 Multi-Tenancy Context

| DO | DON'T |
| :--- | :--- |
| **DO** read tenant context in service logic using [`TenantContextHolder.getTenantId()`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/tenant/TenantContextHolder.java). | **DON'T** pass `tenantId` manually as a method parameter across every internal service and repository method. |
| **DO** configure outgoing `RestClient` or `RestTemplate` beans with [`TenantClientRequestInterceptor`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/tenant/TenantClientRequestInterceptor.java). | **DON'T** manually copy and set `X-Tenant-Id` headers on each individual HTTP client call. |
| **DO** use a `try ... finally { tenantContextProvider.clear(); }` block whenever manually binding context in background threads or message consumers. | **DON'T** call `tenantContextProvider.fill(...)` in an async task without clearing it in `finally` (causes thread pool context leakage). |
| **DO** enable `blackshirt.tenant.require-tenant-id: true` in production microservices that require strict tenant isolation. | **DON'T** silently default `tenantId` to `"default"` or `null` if your service requires tenant-scoped database schemas. |

---

### 2.5 Sensitive Data Masking

| DO | DON'T |
| :--- | :--- |
| **DO** add domain-specific sensitive keys to `blackshirt.logging.masking.additional-masked-fields` in `application.yml`. | **DON'T** hardcode regexes in business services to mask payloads before logging. |
| **DO** register a [`MaskingCustomizer`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/logging/MaskingCustomizer.java) bean if sensitive field names must be loaded dynamically. | **DON'T** disable masking (`enabled: false`) in staging or production environments. |
| **DO** register a custom [`PayloadMasker`](file:///e:/PROJECTS/blackshirt-common-spring/src/main/java/in/blackshirt/common/logging/PayloadMasker.java) bean if specialized algorithms (e.g. PCI-DSS partial card masking) are needed. | **DON'T** log passwords, credit card numbers, OTPs, or API secrets in plain text. |

---

### 2.6 CI/CD & Versioning

| DO | DON'T |
| :--- | :--- |
| **DO** ensure version contains `-SNAPSHOT` in `pom.xml` when merging PRs to the `stage` branch. | **DON'T** merge a release version (e.g. `0.1.4`) to `stage` (the pipeline will fail). |
| **DO** ensure version does NOT contain `SNAPSHOT` in `pom.xml` when merging PRs to the `main` branch. | **DON'T** merge a `-SNAPSHOT` version (e.g. `0.1.4-SNAPSHOT`) to `main` (the pipeline will fail). |
| **DO** ensure all 64+ unit tests pass locally with `mvn test` before submitting PRs. | **DON'T** skip tests with `-DskipTests` in CI pull request validation. |

---

## 3. Code Implementation Patterns

### Pattern 1: Standard Controller Implementation

```java
// ✅ RECOMMENDED PATTERN
package in.blackshirt.order.controller;

import in.blackshirt.common.response.ApiSuccessResponse;
import in.blackshirt.common.response.ResponseFactory;
import in.blackshirt.order.dto.CreateOrderRequest;
import in.blackshirt.order.dto.OrderDto;
import in.blackshirt.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
    public ResponseEntity<ApiSuccessResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDto created = orderService.create(request);
        return responseFactory.created(created, URI.create("/api/v1/orders/" + created.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteOrder(@PathVariable String id) {
        orderService.delete(id);
        return responseFactory.noContent();
    }
}
```

```java
// ❌ ANTI-PATTERN: DO NOT WRITE THIS
@RestController
@RequestMapping("/api/v1/orders")
public class BadOrderController {

    @Autowired private OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String id) { // Missing envelope!
        try {
            log.info("Entering getOrder for id: {}", id); // Redundant logging!
            OrderDto dto = orderService.findById(id);
            return ResponseEntity.ok(dto); // Missing standard envelope metadata!
        } catch (Exception e) {
            log.error("Failed to find order", e); // Duplicate log!
            return ResponseEntity.status(404).build(); // Bypasses centralized error handling!
        }
    }
}
```

---

### Pattern 2: Domain Exception Definition

```java
// ✅ RECOMMENDED PATTERN
package in.blackshirt.order.exception;

import in.blackshirt.common.exception.code.ErrorCode;

public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("ORD_001", "Order was not found for the given identifier", 404),
    INSUFFICIENT_STOCK("ORD_002", "Inventory stock is insufficient for one or more items", 400),
    PAYMENT_FAILED("ORD_003", "Payment authorization failed", 402);

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

---

### Pattern 3: Async Task Tenant Context Binding

```java
// ✅ RECOMMENDED PATTERN
@Service
public class BackgroundNotificationService {

    private final TenantContextProvider tenantContextProvider;
    private final ExecutorService executorService;
    private final NotificationClient notificationClient;

    public BackgroundNotificationService(TenantContextProvider tenantContextProvider,
                                         ExecutorService executorService,
                                         NotificationClient notificationClient) {
        this.tenantContextProvider = tenantContextProvider;
        this.executorService = executorService;
        this.notificationClient = notificationClient;
    }

    public void dispatchAsync(String recipient, String message) {
        // Snapshot context from parent thread
        TenantContext parentContext = tenantContextProvider.getTenantContext();

        executorService.submit(() -> {
            try {
                // Bind to child worker thread
                tenantContextProvider.fill(parentContext);
                notificationClient.send(recipient, message);
            } finally {
                // Always clear in finally block!
                tenantContextProvider.clear();
            }
        });
    }
}
```

