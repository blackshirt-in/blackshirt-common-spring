package in.blackshirt.common.exception;

/**
 * Constants for error response detail keys.
 * Used to provide consistent key names across all error responses.
 */
public final class ErrorDetailKeys {

    private ErrorDetailKeys() {}

    // User-facing information
    /** User-friendly message suitable for UI display */
    public static final String UI_MESSAGE = "uiMessage";

    // Debugging information
    /** Full stack trace of the exception */
    public static final String STACKTRACE = "stacktrace";

    /** Location where exception occurred: ClassName::methodName:lineNumber */
    public static final String LOCATION = "location";

    // Request information
    /** Request body content (requires ContentCachingRequestWrapper) */
    public static final String REQUEST_BODY = "requestBody";

    /** HTTP request headers */
    public static final String REQUEST_HEADERS = "requestHeaders";

    /** HTTP method (GET, POST, etc.) */
    public static final String REQUEST_METHOD = "requestMethod";

    /** Request URI path */
    public static final String REQUEST_URI = "requestUri";

    /** Query parameters from the request */
    public static final String QUERY_PARAMS = "queryParams";

    /** URI template path variables (e.g. /orders/{orderId}) */
    public static final String PATH_VARIABLES = "pathVariables";

    // Distributed tracing & Tenant context
    /** Multi-tenant identifier */
    public static final String TENANT_ID = "tenantId";

    /** Upstream service calling this endpoint */
    public static final String CALLER_SERVICE = "callerService";

    /** Authenticated user ID if present in request context */
    public static final String USER_ID = "userId";

    /** Distributed trace identifier */
    public static final String TRACE_ID = "traceId";

    /** Distributed span identifier */
    public static final String SPAN_ID = "spanId";

    // Validation information
    /** Field-level validation failure map: fieldName -> errorMessage */
    public static final String FIELD_ERRORS = "fieldErrors";

    // Additional context
    /** Simple class name of the exception type */
    public static final String EXCEPTION_TYPE = "exceptionType";

    /** Root cause exception class or message */
    public static final String ROOT_CAUSE = "rootCause";

    /** Timestamp of when the error occurred */
    public static final String TIMESTAMP = "timestamp";
}
