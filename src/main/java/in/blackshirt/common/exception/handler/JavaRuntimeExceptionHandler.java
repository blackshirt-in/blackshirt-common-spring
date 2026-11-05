package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.code.RuntimeErrorCode;
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
 * 4th in the common exception handling pipeline.
 * Catches common Java core language / runtime exceptions before they hit the catch-all fallback.
 * Uses {@link RuntimeErrorCode} (ERR-RT-001..099).
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.RUNTIME_HANDLER)
public class JavaRuntimeExceptionHandler extends AbstractExceptionHandler {

    public JavaRuntimeExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        super(tracer, request, properties);
    }

    public JavaRuntimeExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties, PayloadMasker payloadMasker) {
        super(tracer, request, properties, payloadMasker);
    }

    @ExceptionHandler(NullPointerException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleNullPointer(NullPointerException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.NULL_POINTER,
                RuntimeErrorCode.NULL_POINTER.getDefaultMessage(),
                details,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.ILLEGAL_ARGUMENT,
                ex.getMessage() != null ? ex.getMessage() : RuntimeErrorCode.ILLEGAL_ARGUMENT.getDefaultMessage(),
                details,
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.ILLEGAL_STATE,
                ex.getMessage() != null ? ex.getMessage() : RuntimeErrorCode.ILLEGAL_STATE.getDefaultMessage(),
                details,
                HttpStatus.CONFLICT,
                ex
        );
    }

    @ExceptionHandler(ArithmeticException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleArithmetic(ArithmeticException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.ARITHMETIC_ERROR,
                "An arithmetic calculation error occurred: " + (ex.getMessage() != null ? ex.getMessage() : "division or overflow"),
                details,
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleIndexOutOfBounds(IndexOutOfBoundsException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.INDEX_OUT_OF_BOUNDS,
                RuntimeErrorCode.INDEX_OUT_OF_BOUNDS.getDefaultMessage(),
                details,
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleUnsupportedOperation(UnsupportedOperationException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                RuntimeErrorCode.UNSUPPORTED_OPERATION,
                ex.getMessage() != null ? ex.getMessage() : RuntimeErrorCode.UNSUPPORTED_OPERATION.getDefaultMessage(),
                details,
                HttpStatus.NOT_IMPLEMENTED,
                ex
        );
    }
}

