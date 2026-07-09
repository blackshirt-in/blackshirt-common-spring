package in.blackshirt.common.response;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record ApiErrorResponse<T>(
        String serviceName,
        String errorCode,
        String errorMessage,
        Map<String, Object> details,
        Instant timestamp,
        String traceId,
        String spanId,
        Optional<String> path
) implements BaseResponse<T> {

    public static <T> ApiErrorResponse<T> error(
            String serviceName,
            ErrorCode errorCode,
            String customMessage,
            Map<String, Object> details,
            String traceId,
            String spanId,
            String path
    ) {
        return new ApiErrorResponse<T>(
                serviceName,
                errorCode.getCode(),
                customMessage != null ? customMessage : errorCode.getDefaultMessage(),
                details,
                Instant.now(),
                traceId,
                spanId,
                Optional.ofNullable(path)
        );
    }
}
