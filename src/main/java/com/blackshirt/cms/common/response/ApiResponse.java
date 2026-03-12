package com.blackshirt.cms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    List<ApiError> errors,
    String traceId,
    LocalDateTime timestamp
) {

    public ApiResponse(boolean success, T data, List<ApiError> errors) {
        this(success, data, errors, getOrGenerateTraceId(), LocalDateTime.now());
    }

    private static String getOrGenerateTraceId() {
        String existingTraceId = MDC.get("traceId");
        return (existingTraceId != null && !existingTraceId.isEmpty()) ? existingTraceId : UUID.randomUUID().toString();
    }

    // Static factory methods for easier construction

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        List<ApiError> errors = new ArrayList<>();
        errors.add(error);
        return new ApiResponse<>(false, null, errors);
    }

    public static <T> ApiResponse<T> error(List<ApiError> errors) {
        return new ApiResponse<>(false, null, errors);
    }

    public static <T> ApiResponse<T> error(String code, String message, String details) {
        return error(new ApiError(code, message, details));
    }
}
