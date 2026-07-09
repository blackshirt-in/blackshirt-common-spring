package in.blackshirt.common.response;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.logging.TraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Central factory for building consistent API responses.
 *
 * <p>Injects infrastructure beans ({@link Tracer}, {@link BlackshirtProperties}) once
 * so that controllers never need to deal with trace IDs, service names, or timestamps.
 *
 * <h2>Usage in Controllers</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/orders")
 * public class OrderController {
 *
 *     private final ResponseFactory responseFactory;
 *     private final OrderService orderService;
 *
 *     public OrderController(ResponseFactory responseFactory, OrderService orderService) {
 *         this.responseFactory = responseFactory;
 *         this.orderService = orderService;
 *     }
 *
 *     @GetMapping("/{id}")
 *     public ResponseEntity<ApiSuccessResponse<OrderDto>> getOrder(@PathVariable String id) {
 *         OrderDto order = orderService.findById(id);
 *         return responseFactory.success(order, "Order retrieved");
 *     }
 *
 *     @PostMapping
 *     public ResponseEntity<ApiSuccessResponse<OrderDto>> create(@RequestBody CreateOrderRequest req) {
 *         OrderDto order = orderService.create(req);
 *         return responseFactory.success(order, "Order created", HttpStatus.CREATED);
 *     }
 *
 *     @DeleteMapping("/{id}")
 *     public ResponseEntity<ApiErrorResponse<?>> cancelExpiredOrder(@PathVariable String id) {
 *         // Explicit error response (alternative to throwing an exception)
 *         return responseFactory.error(OrderErrorCode.ORDER_EXPIRED);
 *     }
 * }
 * }</pre>
 *
 * @see ApiSuccessResponse
 * @see ApiErrorResponse
 * @see ErrorCode
 */
@Component
public class ResponseFactory {

    private final Tracer tracer;
    private final BlackshirtProperties properties;

    public ResponseFactory(Tracer tracer, BlackshirtProperties properties) {
        this.tracer = tracer;
        this.properties = properties;
    }

    // ────────────────────── Success Responses ──────────────────────

    /**
     * Builds a {@code 200 OK} success response with the given data.
     */
    public <T> ResponseEntity<ApiSuccessResponse<T>> success(T data) {
        return success(data, "Success", null, HttpStatus.OK);
    }

    /**
     * Builds a {@code 200 OK} success response with the given data and message.
     */
    public <T> ResponseEntity<ApiSuccessResponse<T>> success(T data, String message) {
        return success(data, message, null, HttpStatus.OK);
    }

    /**
     * Builds a success response with the given data, message, and HTTP status.
     * <p>Use for non-200 successes such as {@link HttpStatus#CREATED}.
     */
    public <T> ResponseEntity<ApiSuccessResponse<T>> success(T data, String message, HttpStatus status) {
        return success(data, message, null, status);
    }

    /**
     * Builds a success response with full control over all fields.
     *
     * @param data     the response payload
     * @param message  a human-readable message describing the result
     * @param metadata optional metadata map (e.g., pagination info)
     * @param status   the HTTP status code
     */
    public <T> ResponseEntity<ApiSuccessResponse<T>> success(T data, String message,
                                                              Map<String, Object> metadata, HttpStatus status) {
        var traceIds = TraceContext.current(tracer);
        var response = new ApiSuccessResponse<>(
                properties.log().serviceName(),
                data,
                message,
                metadata,
                Instant.now(),
                traceIds.traceId(),
                traceIds.spanId()
        );
        return ResponseEntity.status(status).body(response);
    }

    // ────────────────────── Error Responses ──────────────────────

    /**
     * Builds an error response using the error code's default message.
     */
    public <T> ResponseEntity<ApiErrorResponse<T>> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getDefaultMessage(), null);
    }

    /**
     * Builds an error response with a custom message.
     */
    public <T> ResponseEntity<ApiErrorResponse<T>> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, null);
    }

    /**
     * Builds an error response with full control over all fields.
     *
     * @param errorCode the business error code
     * @param message   a human-readable error message (falls back to error code default if null)
     * @param details   optional key-value details providing additional context
     */
    public <T> ResponseEntity<ApiErrorResponse<T>> error(ErrorCode errorCode, String message,
                                                          Map<String, Object> details) {
        var traceIds = TraceContext.current(tracer);
        String path = resolveRequestPath();
        var response = new ApiErrorResponse<T>(
                properties.log().serviceName(),
                errorCode.getCode(),
                message != null ? message : errorCode.getDefaultMessage(),
                details,
                Instant.now(),
                traceIds.traceId(),
                traceIds.spanId(),
                Optional.ofNullable(path)
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Resolves the current HTTP request path, if available.
     * Returns {@code null} when called outside a web request context.
     */
    private String resolveRequestPath() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {
            // Not in a web request context — path is unknown
        }
        return null;
    }
}
