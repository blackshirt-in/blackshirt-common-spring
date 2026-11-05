package in.blackshirt.common.exception.code;

/**
 * Contract for all error codes used across Blackshirt microservices and framework modules.
 *
 * <p>This interface defines the shape of an error code that can be used with
 * response models and the exception handling framework.
 *
 * <h2>Standard Framework Error Series</h2>
 * <ul>
 *   <li><b>HTTP / Web:</b> {@code ERR-HTTP-001..099} — defined by {@link HttpErrorCode}</li>
 *   <li><b>Database / Persistence:</b> {@code ERR-DB-001..099} — defined by {@link DatabaseErrorCode}</li>
 *   <li><b>Core Java Runtime:</b> {@code ERR-RT-001..099} — defined by {@link RuntimeErrorCode}</li>
 *   <li><b>System Fallback:</b> {@code ERR-SYS-001..099} — defined by {@link SystemErrorCode}</li>
 * </ul>
 *
 * <h2>Business / Application Errors (Client Services)</h2>
 * <p>Client services <b>must provide their own</b> domain error codes by implementing this
 * interface with their unique service prefix:
 * <br>Format: {@code {SERVICE_PREFIX}-{NNN}} (e.g. {@code ORD-001}, {@code PAY-001}, {@code USR-001}).
 *
 * <h2>Usage Example in Microservice</h2>
 * <pre>{@code
 * public enum OrderErrorCode implements ErrorCode {
 *
 *     INSUFFICIENT_STOCK("ORD-001", "Insufficient stock for the requested item", 409),
 *     ORDER_EXPIRED("ORD-002", "Order has expired and can no longer be processed", 410),
 *     DUPLICATE_ORDER("ORD-003", "An order with this reference already exists", 409),
 *     INVALID_COUPON("ORD-004", "The provided coupon code is invalid or expired", 422);
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
 * @see HttpErrorCode
 * @see DatabaseErrorCode
 * @see RuntimeErrorCode
 * @see SystemErrorCode
 */
public interface ErrorCode {

    /**
     * The unique business error code string.
     * <p>Standard framework errors follow {@code ERR-HTTP-xxx}, {@code ERR-DB-xxx},
     * {@code ERR-RT-xxx}, or {@code ERR-SYS-xxx}.
     * Service domain errors follow {@code {SERVICE_PREFIX}-{NNN}}.
     *
     * @return the error code string, e.g. {@code "ORD-001"}, {@code "ERR-HTTP-001"}
     */
    String getCode();

    /**
     * The default human-readable error message safe for API consumers.
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

