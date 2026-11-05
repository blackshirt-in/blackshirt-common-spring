package in.blackshirt.common.exception.code;

/**
 * Standard HTTP, Web protocol, and request validation error codes.
 * Prefix: {@code ERR-HTTP-}, Series Range: {@code 001..099}.
 */
public enum HttpErrorCode implements ErrorCode {

    BAD_REQUEST("ERR-HTTP-001", "The request is malformed or contains invalid parameters", 400),
    VALIDATION_FAILED("ERR-HTTP-002", "Validation failed for one or more fields", 400),
    UNAUTHORIZED("ERR-HTTP-003", "Authentication is required to access this resource", 401),
    FORBIDDEN("ERR-HTTP-004", "You do not have permission to perform this action", 403),
    RESOURCE_NOT_FOUND("ERR-HTTP-005", "The requested resource was not found", 404),
    METHOD_NOT_ALLOWED("ERR-HTTP-006", "The HTTP method is not supported for this endpoint", 405),
    NOT_ACCEPTABLE("ERR-HTTP-007", "The requested media type is not acceptable", 406),
    REQUEST_TIMEOUT("ERR-HTTP-008", "The server timed out waiting for the request", 408),
    CONFLICT("ERR-HTTP-009", "The request conflicts with the current state of the resource", 409),
    GONE("ERR-HTTP-010", "The requested resource is no longer available", 410),
    UNSUPPORTED_MEDIA_TYPE("ERR-HTTP-011", "The request payload format is not supported", 415),
    UNPROCESSABLE_ENTITY("ERR-HTTP-012", "The request is well-formed but contains semantic errors", 422),
    TOO_MANY_REQUESTS("ERR-HTTP-013", "Too many requests – please slow down", 429),
    INTERNAL_SERVER_ERROR("ERR-HTTP-014", "An internal HTTP processing error occurred", 500),
    NOT_IMPLEMENTED("ERR-HTTP-015", "The requested functionality is not implemented", 501),
    BAD_GATEWAY("ERR-HTTP-016", "The server received an invalid response from an upstream service", 502),
    SERVICE_UNAVAILABLE("ERR-HTTP-017", "The service is temporarily unavailable – please retry later", 503),
    GATEWAY_TIMEOUT("ERR-HTTP-018", "The server timed out waiting for an upstream service", 504);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    HttpErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}

