package in.blackshirt.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Sealed hierarchy for all API responses.
 * Only Success and Error variants are permitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface BaseResponse<T> permits ApiSuccessResponse, ApiErrorResponse {

    /** Service name */
    String serviceName();

    /** UTC time when the response was created */
    Instant timestamp();

    /** Distributed trace ID (taken from current span) */
    String traceId();

    /** Distributed span ID (current span) */
    String spanId();

    default boolean isSuccess() { return this instanceof ApiSuccessResponse; }
    default boolean isError() { return this instanceof ApiErrorResponse; }
}
