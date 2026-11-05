package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.ErrorDetailKeys;
import in.blackshirt.common.exception.code.ErrorCode;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.logging.TraceContext;
import in.blackshirt.common.response.ApiErrorResponse;
import in.blackshirt.common.tenant.TenantContext;
import in.blackshirt.common.tenant.TenantContextHolder;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Common base class for all Blackshirt exception handlers.
 * Standardizes trace correlation, error detail extraction, tenant context propagation,
 * response building, and logging.
 */
public abstract class AbstractExceptionHandler {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Tracer tracer;
    protected final HttpServletRequest request;
    protected final BlackshirtProperties properties;
    protected final PayloadMasker payloadMasker;

    protected AbstractExceptionHandler(Tracer tracer,
                                     HttpServletRequest request,
                                     BlackshirtProperties properties,
                                     PayloadMasker payloadMasker) {
        this.tracer = tracer;
        this.request = request;
        this.properties = properties != null ? properties : new BlackshirtProperties();
        this.payloadMasker = payloadMasker;
    }

    protected AbstractExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        this(tracer, request, properties, null);
    }

    /**
     * Builds standardized diagnostic details for an exception and the current request context,
     * including automatic tenantId, callerService, userId, and path variables.
     */
    protected Map<String, Object> buildBaseErrorDetails(Throwable ex) {
        Map<String, Object> details = new HashMap<>();
        if (ex != null) {
            String message = null;
            try {
                message = ex.getMessage();
            } catch (Throwable ignored) {
                message = ex.getClass().getSimpleName();
            }
            details.put(ErrorDetailKeys.UI_MESSAGE, message);
            details.put(ErrorDetailKeys.LOCATION, extractLocation(ex));
            details.put(ErrorDetailKeys.EXCEPTION_TYPE, ex.getClass().getSimpleName());

            Throwable rootCause = extractRootCause(ex);
            if (rootCause != null && rootCause != ex) {
                String causeMsg = null;
                try {
                    causeMsg = rootCause.getMessage();
                } catch (Throwable ignored) {
                    causeMsg = rootCause.getClass().getSimpleName();
                }
                details.put(ErrorDetailKeys.ROOT_CAUSE, rootCause.getClass().getSimpleName() + ": " + causeMsg);
            }

            if (properties.getExceptionHandler().isIncludeStackTrace()) {
                details.put(ErrorDetailKeys.STACKTRACE, extractStackTrace(ex));
            }
        }

        // Distributed tracing correlation
        var traceIds = TraceContext.current(tracer);
        if (traceIds.traceId() != null && !traceIds.traceId().isBlank()) {
            details.put(ErrorDetailKeys.TRACE_ID, traceIds.traceId());
        }
        if (traceIds.spanId() != null && !traceIds.spanId().isBlank()) {
            details.put(ErrorDetailKeys.SPAN_ID, traceIds.spanId());
        }

        // Automatic Tenant Context & Caller Service enrichment
        String tenantId = TenantContextHolder.getTenantId();
        if ((tenantId == null || tenantId.isBlank()) && properties.getTenant() != null) {
            tenantId = MDC.get(properties.getTenant().getTenantMdcKey());
        }
        if (tenantId != null && !tenantId.isBlank()) {
            details.put(ErrorDetailKeys.TENANT_ID, tenantId);
        }

        String callerService = TenantContextHolder.getCallerService();
        if ((callerService == null || callerService.isBlank() || "unknown-service".equals(callerService)) && properties.getTenant() != null) {
            callerService = MDC.get(properties.getTenant().getServiceMdcKey());
        }
        if (callerService != null && !callerService.isBlank()) {
            details.put(ErrorDetailKeys.CALLER_SERVICE, callerService);
        }

        String userId = TenantContextHolder.getUserId();
        if (userId != null && !userId.isBlank()) {
            details.put(ErrorDetailKeys.USER_ID, userId);
        } else if (properties.getTenant() != null) {
            String mdcUser = MDC.get(properties.getTenant().getUserMdcKey());
            if (mdcUser != null && !mdcUser.isBlank()) {
                details.put(ErrorDetailKeys.USER_ID, mdcUser);
            }
        }

        // Request details, path variables, query params and headers
        HttpServletRequest currentReq = resolveCurrentRequest();
        if (currentReq != null) {
            Map<String, String> pathVars = extractPathVariables(currentReq);
            if (!pathVars.isEmpty()) {
                details.put(ErrorDetailKeys.PATH_VARIABLES, pathVars);
            }
            details.put(ErrorDetailKeys.REQUEST_HEADERS, maskHeaders(extractHeaders(currentReq)));
            details.put(ErrorDetailKeys.REQUEST_METHOD, currentReq.getMethod());
            details.put(ErrorDetailKeys.REQUEST_URI, currentReq.getRequestURI());
            details.put(ErrorDetailKeys.QUERY_PARAMS, maskQueryParams(extractQueryParams(currentReq)));
        }

        details.put(ErrorDetailKeys.TIMESTAMP, Instant.now().toString());
        return details;
    }

    /**
     * Constructs a standardized {@link ResponseEntity} wrapping an {@link ApiErrorResponse}.
     */
    protected <T> ResponseEntity<ApiErrorResponse<T>> buildErrorResponse(
            ErrorCode errorCode,
            String customMessage,
            Map<String, Object> details,
            HttpStatus status,
            Throwable ex
    ) {
        var traceIds = TraceContext.current(tracer);
        String serviceName = properties.getServiceName();
        String path = resolveRequestUri();

        ApiErrorResponse<T> error = ApiErrorResponse.error(
                serviceName,
                errorCode,
                customMessage,
                details,
                traceIds.traceId(),
                traceIds.spanId(),
                path
        );

        logException(status, error.errorCode(), error.errorMessage(), traceIds.traceId(), path, ex);
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Overload to build error response using default base error details.
     */
    protected <T> ResponseEntity<ApiErrorResponse<T>> buildErrorResponse(
            ErrorCode errorCode,
            Throwable ex,
            HttpStatus status
    ) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(errorCode, ex != null ? ex.getMessage() : errorCode.getDefaultMessage(), details, status, ex);
    }

    /**
     * Unified logging for handled exceptions (WARN for 4xx client errors, ERROR for 5xx server errors).
     * Correlation fields (traceId, tenantId, callerService) are formatted via SLF4J MDC and standard logging pattern.
     */
    protected void logException(HttpStatus status, String code, String message, String traceId, String path, Throwable ex) {
        String safePath = path != null ? path : "?";
        if (status.is4xxClientError()) {
            log.warn("{}: code={}, status={}, path={}, message={}",
                    getClass().getSimpleName(), code, status.value(), safePath, message);
        } else {
            log.error("{}: code={}, status={}, path={}, message={}",
                    getClass().getSimpleName(), code, status.value(), safePath, message, ex);
        }
    }

    protected static Map<String, String> extractPathVariables(HttpServletRequest request) {
        return in.blackshirt.common.logging.HttpLoggingUtils.extractPathVariables(request);
    }

    protected Map<String, String> maskHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        if (payloadMasker != null) {
            return payloadMasker.maskHeaders(headers);
        }
        Set<String> sensitiveHeaders = (properties.getLogging() != null && properties.getLogging().getMasking() != null)
                ? properties.getLogging().getMasking().getEffectiveMaskedHeaders()
                : Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token");

        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((k, v) -> {
            if (k != null && sensitiveHeaders.contains(k.toLowerCase())) {
                masked.put(k, "***");
            } else {
                masked.put(k, v);
            }
        });
        return masked;
    }

    protected Map<String, String> maskQueryParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> sensitiveKeys = (properties.getLogging() != null && properties.getLogging().getMasking() != null)
                ? properties.getLogging().getMasking().getEffectiveMaskedFields()
                : Set.of("password", "token", "secret", "apiKey", "access_token");

        Map<String, String> masked = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (k != null && sensitiveKeys.contains(k.toLowerCase())) {
                masked.put(k, "***");
            } else {
                masked.put(k, v);
            }
        });
        return masked;
    }

    protected static String extractStackTrace(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    protected static String extractLocation(Throwable ex) {
        if (ex == null) {
            return "unknown";
        }
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];
            return String.format("%s::%s:%d",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getLineNumber());
        }
        return "unknown";
    }

    protected static Throwable extractRootCause(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    protected static Map<String, String> extractHeaders(HttpServletRequest request) {
        return in.blackshirt.common.logging.HttpLoggingUtils.extractHeaders(request);
    }

    protected static Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        if (request == null || request.getParameterMap() == null) {
            return params;
        }
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, String.join(",", values));
            }
        });
        return params;
    }

    protected HttpServletRequest resolveCurrentRequest() {
        if (this.request != null) {
            return this.request;
        }
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignored) {
            // Not in a web context
        }
        return null;
    }

    protected String resolveRequestUri() {
        HttpServletRequest currentReq = resolveCurrentRequest();
        return currentReq != null ? currentReq.getRequestURI() : null;
    }
}
