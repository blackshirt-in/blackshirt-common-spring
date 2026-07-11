package in.blackshirt.common.exception;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.response.ApiErrorResponse;
import in.blackshirt.common.response.StandardErrorCode;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global fallback exception handler provided by the common library.
 *
 * <p>
 * Runs at {@link Ordered#LOWEST_PRECEDENCE} so that client services can define
 * their own {@link RestControllerAdvice} handlers at a higher priority
 * (e.g., {@code @Order(Ordered.HIGHEST_PRECEDENCE)}) to intercept
 * domain-specific
 * exceptions <em>before</em> this handler.
 *
 * <h2>Extension Pattern</h2>
 * 
 * <pre>{@code
 * @RestControllerAdvice
 * @Order(Ordered.HIGHEST_PRECEDENCE)
 * public class OrderExceptionHandler {
 *
 *     @ExceptionHandler(PaymentFailedException.class)
 *     public ResponseEntity<ApiErrorResponse<?>> handle(PaymentFailedException ex) {
 *         // custom handling — this runs BEFORE GlobalExceptionHandler
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Any exception not caught by a higher-priority handler falls through to
 * {@link #handleBaseException} or {@link #handleUnexpected} here.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Tracer tracer;
    private final HttpServletRequest request;
    private final BlackshirtProperties properties;

    public GlobalExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        this.tracer = tracer;
        this.request = request;
        this.properties = properties;
    }

    @ExceptionHandler(BaseException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleBaseException(BaseException ex) {
        ApiErrorResponse<T> error = ErrorResponseConverter.fromException(
                properties.getLog().getServiceName(), ex, tracer, request.getRequestURI());
        log.error("BaseException handled: code={}, traceId={}, path={}",
                error.errorCode(), error.traceId(), error.path().orElse("?"));
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleUnexpected(Exception ex) {
        // Wrap into a generic internal error
        InternalServerException baseEx = new InternalServerException(
                StandardErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        ApiErrorResponse<T> error = ErrorResponseConverter.fromException(
                properties.getLog().getServiceName(), baseEx, tracer, request.getRequestURI());
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(error);
    }
}
