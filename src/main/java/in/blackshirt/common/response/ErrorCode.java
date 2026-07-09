package in.blackshirt.common.response;

/**
 * Contract for all error codes used across Blackshirt microservices.
 *
 * <p>This interface defines the shape of an error code that can be used with
 * {@link BaseResponse}, {@link ApiErrorResponse}, and the exception handling framework.
 * Client services implement this as an {@code enum} to define their domain-specific errors.
 *
 * <h2>Code Convention</h2>
 * <ul>
 *   <li><b>Standard HTTP errors:</b> {@code ERR_000xxx} — provided by {@link StandardErrorCode}.
 *       These cover generic HTTP error codes (400, 401, 404, 500, etc.) and should <em>not</em>
 *       be redefined by client services.</li>
 *   <li><b>Service-specific errors:</b> {@code {SERVICE_PREFIX}_{NNN}} — defined by each
 *       client service using a unique 2-4 letter prefix.
 *       <br>Examples: {@code ORD_001} (Order service), {@code PAY_001} (Payment service),
 *       {@code USR_001} (User service).</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public enum OrderErrorCode implements ErrorCode {
 *
 *     INSUFFICIENT_STOCK("ORD_001", "Insufficient stock for the requested item", 409),
 *     ORDER_EXPIRED("ORD_002", "Order has expired and can no longer be processed", 410),
 *     DUPLICATE_ORDER("ORD_003", "An order with this reference already exists", 409),
 *     INVALID_COUPON("ORD_004", "The provided coupon code is invalid or expired", 422);
 *
 *     private final String code;
 *     private final String defaultMessage;
 *     private final int httpStatus;
 *
 *     OrderErrorCode(String code, String defaultMessage, int httpStatus) {
 *         this.code = code;
 *         this.defaultMessage = defaultMessage;
 *         this.httpStatus = httpStatus;
 *     }
 *
 *     @Override public String getCode() { return code; }
 *     @Override public String getDefaultMessage() { return defaultMessage; }
 *     @Override public int getHttpStatus() { return httpStatus; }
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Choose a unique prefix per service to avoid code collisions across the platform.</li>
 *   <li>Use sequential numbering within each prefix ({@code ORD_001}, {@code ORD_002}, ...).</li>
 *   <li>Map each error code to the most semantically appropriate HTTP status code.</li>
 *   <li>Write default messages for end-user consumption — avoid exposing internal details.</li>
 *   <li>For generic HTTP errors (400, 401, 403, 404, 500), prefer {@link StandardErrorCode}
 *       instead of defining duplicates.</li>
 * </ul>
 *
 * @see StandardErrorCode
 * @see BaseException
 * @see ApiErrorResponse
 */
public interface ErrorCode {

    /**
     * The unique business error code string.
     * <p>Must follow the convention: {@code {SERVICE_PREFIX}_{NNN}} for service-specific errors,
     * or {@code ERR_000xxx} for standard HTTP errors.
     *
     * @return the error code, e.g. {@code "ORD_001"}, {@code "ERR_000404"}
     */
    String getCode();

    /**
     * The default human-readable error message.
     * <p>This message is safe for end-user consumption and should describe the error
     * without exposing internal implementation details.
     *
     * @return the default message, e.g. {@code "Insufficient stock for the requested item"}
     */
    String getDefaultMessage();

    /**
     * The HTTP status code to use when this error is returned in a REST response.
     *
     * @return the HTTP status code, e.g. {@code 409}, {@code 404}, {@code 500}
     */
    int getHttpStatus();
}
