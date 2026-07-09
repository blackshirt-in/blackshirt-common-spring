package in.blackshirt.common.response;

import java.time.Instant;
import java.util.Map;

public record ApiSuccessResponse<T>(
        String serviceName,
        T data,
        String message,
        Map<String, Object> metadata,
        Instant timestamp,
        String traceId,
        String spanId
) implements BaseResponse<T> {

    public static <T> ApiSuccessResponse<T> ok(
            String serviceName,
            T data,
            String message,
            Map<String, Object> metadata,
            String traceId,
            String spanId
    ) {

        return new ApiSuccessResponse<>(
                serviceName,
                data,
                message,
                metadata,
                Instant.now(),
                traceId,
                spanId
        );
    }
}
