package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.BaseException;
import in.blackshirt.common.exception.code.ErrorCode;
import in.blackshirt.common.exception.code.HttpErrorCode;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 3rd in the common exception handling pipeline.
 * Handles business, domain, and application exceptions rooted in {@link BaseException},
 * as well as Spring {@link ResponseStatusException}.
 *
 * <p>Consuming microservices define their own domain error codes (e.g. {@code ORD-001}, {@code PAY-001})
 * by implementing {@link ErrorCode} and passing them to their {@link BaseException} subclasses.
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.BUSINESS_HANDLER)
public class BusinessExceptionHandler extends AbstractExceptionHandler {

    public BusinessExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        super(tracer, request, properties);
    }

    public BusinessExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties, PayloadMasker payloadMasker) {
        super(tracer, request, properties, payloadMasker);
    }

    @ExceptionHandler(BaseException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleBaseException(BaseException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }

        int statusCode = ex.getErrorCode().getHttpStatus();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return buildErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                details,
                status,
                ex
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorCode errorCode = mapStatusToErrorCode(status);
        String message = ex.getReason() != null ? ex.getReason() : errorCode.getDefaultMessage();

        return buildErrorResponse(
                errorCode,
                message,
                details,
                status,
                ex
        );
    }

    private ErrorCode mapStatusToErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> HttpErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> HttpErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> HttpErrorCode.FORBIDDEN;
            case NOT_FOUND -> HttpErrorCode.RESOURCE_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpErrorCode.METHOD_NOT_ALLOWED;
            case NOT_ACCEPTABLE -> HttpErrorCode.NOT_ACCEPTABLE;
            case REQUEST_TIMEOUT -> HttpErrorCode.REQUEST_TIMEOUT;
            case CONFLICT -> HttpErrorCode.CONFLICT;
            case GONE -> HttpErrorCode.GONE;
            case UNSUPPORTED_MEDIA_TYPE -> HttpErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case UNPROCESSABLE_ENTITY -> HttpErrorCode.UNPROCESSABLE_ENTITY;
            case TOO_MANY_REQUESTS -> HttpErrorCode.TOO_MANY_REQUESTS;
            case NOT_IMPLEMENTED -> HttpErrorCode.NOT_IMPLEMENTED;
            case BAD_GATEWAY -> HttpErrorCode.BAD_GATEWAY;
            case SERVICE_UNAVAILABLE -> HttpErrorCode.SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT -> HttpErrorCode.GATEWAY_TIMEOUT;
            default -> HttpErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}

