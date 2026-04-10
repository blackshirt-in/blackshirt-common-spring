package com.blackshirt.cms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single error returned inside {@link ApiResponse}.
 *
 * <ul>
 *   <li>{@code code}    – machine-readable error code (e.g. {@code VALIDATION_ERROR}, {@code AUTH_FAILED})</li>
 *   <li>{@code message} – human-readable description of the error</li>
 *   <li>{@code details} – optional extra context (stack trace excerpt, raw cause, etc.)</li>
 *   <li>{@code field}   – optional field name that triggered a validation error</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    String details,
    String field
) {
    /** Convenience constructor without field – for non-validation errors. */
    public ApiError(String code, String message, String details) {
        this(code, message, details, null);
    }

    /** Factory: generic error without field context. */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null);
    }

    /** Factory: error with extra details. */
    public static ApiError of(String code, String message, String details) {
        return new ApiError(code, message, details, null);
    }

    /** Factory: field-level validation error. */
    public static ApiError fieldError(String field, String message) {
        return new ApiError("VALIDATION_ERROR", message, null, field);
    }
}
