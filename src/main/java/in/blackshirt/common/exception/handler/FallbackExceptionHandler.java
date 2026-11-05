package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.code.SystemErrorCode;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Final fallback in the exception handling pipeline.
 * Runs at {@link ExceptionHandlerOrder#FALLBACK_HANDLER} (lowest precedence).
 * Catches any unhandled {@link Exception} or {@link Throwable} to ensure client callers always
 * receive a structured {@link ApiErrorResponse} and prevents leaking whitelabel/raw traces.
 * Uses {@link SystemErrorCode} (ERR-SYS-001..099).
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.FALLBACK_HANDLER)
public class FallbackExceptionHandler extends AbstractExceptionHandler {

    public FallbackExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        super(tracer, request, properties);
    }

    public FallbackExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties, PayloadMasker payloadMasker) {
        super(tracer, request, properties, payloadMasker);
    }

    @ExceptionHandler(Exception.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleUnexpected(Exception ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                SystemErrorCode.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : SystemErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                details,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
    }

    @ExceptionHandler(Throwable.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleThrowable(Throwable ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                SystemErrorCode.CRITICAL_FAILURE,
                ex.getMessage() != null ? ex.getMessage() : SystemErrorCode.CRITICAL_FAILURE.getDefaultMessage(),
                details,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
    }
}

