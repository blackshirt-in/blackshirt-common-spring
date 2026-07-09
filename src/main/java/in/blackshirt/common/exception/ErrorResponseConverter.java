package in.blackshirt.common.exception;

import in.blackshirt.common.logging.TraceContext;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;

import java.time.Instant;
import java.util.Optional;

/**
 * Pure converter: turns a BaseException into an ErrorResponse.
 * Can be used in global exception handlers or wherever needed.
 */
public final class ErrorResponseConverter {

    private ErrorResponseConverter() {}

    public static <T> ApiErrorResponse<T> fromException(String serviceName, BaseException ex, Tracer tracer, String path) {
        var traceIds = TraceContext.current(tracer);
        return new ApiErrorResponse<T>(
                serviceName,
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getDetails(),
                Instant.now(),
                traceIds.traceId(),
                traceIds.spanId(),
                Optional.ofNullable(path)
        );
    }
}
