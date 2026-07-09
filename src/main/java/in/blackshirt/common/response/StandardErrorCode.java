package in.blackshirt.common.response;

public enum StandardErrorCode implements ErrorCode {
    // ---- 4xx Client errors ----
    BAD_REQUEST("ERR_000400", "The request is malformed or contains invalid parameters", 400),
    UNAUTHORIZED("ERR_000401", "Authentication is required to access this resource", 401),
    FORBIDDEN("ERR_000403", "You do not have permission to perform this action", 403),
    RESOURCE_NOT_FOUND("ERR_000404", "The requested resource was not found", 404),
    METHOD_NOT_ALLOWED("ERR_000405", "The HTTP method is not supported for this endpoint", 405),
    NOT_ACCEPTABLE("ERR_000406", "The requested media type is not acceptable", 406),
    REQUEST_TIMEOUT("ERR_000408", "The server timed out waiting for the request", 408),
    CONFLICT("ERR_000409", "The request conflicts with the current state of the resource", 409),
    GONE("ERR_000410", "The requested resource is no longer available", 410),
    UNSUPPORTED_MEDIA_TYPE("ERR_000415", "The request payload format is not supported", 415),
    UNPROCESSABLE_ENTITY("ERR_000422", "The request is well-formed but contains semantic errors", 422),
    TOO_MANY_REQUESTS("ERR_000429", "Too many requests – please slow down", 429),

    // ---- 5xx Server errors ----
    INTERNAL_SERVER_ERROR("ERR_000500", "An unexpected system error occurred", 500),
    NOT_IMPLEMENTED("ERR_000501", "The requested functionality is not implemented", 501),
    BAD_GATEWAY("ERR_000502", "The server received an invalid response from an upstream service", 502),
    SERVICE_UNAVAILABLE("ERR_000503", "The service is temporarily unavailable – please retry later", 503),
    GATEWAY_TIMEOUT("ERR_000504", "The server timed out waiting for an upstream service", 504);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    StandardErrorCode(String code, String defaultMessage, int httpStatus) {
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
