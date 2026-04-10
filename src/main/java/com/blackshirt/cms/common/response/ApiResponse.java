package com.blackshirt.cms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralized API response envelope used by all CMS services.
 *
 * <pre>
 * {
 *   "success":   true | false,
 *   "data":      &lt;payload – null on error&gt;,
 *   "errors":    [ { code, message, details, field } ] – null on success,
 *   "traceId":   "uuid or MDC value",
 *   "timestamp": "2026-04-10T11:30:00"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    List<ApiError> errors,
    String traceId,
    LocalDateTime timestamp
) {

    /** Canonical constructor – resolves traceId and timestamp automatically. */
    public ApiResponse(boolean success, T data, List<ApiError> errors) {
        this(success, data, errors, resolveTraceId(), LocalDateTime.now());
    }

    private static String resolveTraceId() {
        String mdcTrace = MDC.get("traceId");
        return (mdcTrace != null && !mdcTrace.isEmpty()) ? mdcTrace : UUID.randomUUID().toString();
    }

    // ─── Success factories ──────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Wraps the payload in a 200 OK {@link ResponseEntity}. */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(success(data));
    }

    /** Wraps the payload in a 201 Created {@link ResponseEntity}. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(success(data));
    }

    // ─── Error factories ────────────────────────────────────────────────────

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

    /** Wraps a single error in a {@link ResponseEntity} with the given HTTP status. */
    public static <T> ResponseEntity<ApiResponse<T>> errorResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(error(ApiError.of(code, message)));
    }

    /** Wraps a list of errors in a {@link ResponseEntity} with the given HTTP status. */
    public static <T> ResponseEntity<ApiResponse<T>> errorResponse(HttpStatus status, List<ApiError> errors) {
        return ResponseEntity.status(status).body(error(errors));
    }
}
