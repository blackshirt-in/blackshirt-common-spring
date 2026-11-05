package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.ErrorDetailKeys;
import in.blackshirt.common.exception.code.HttpErrorCode;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 1st in the common exception handling pipeline.
 * Handles HTTP / Spring Web MVC protocol, routing, binding, and payload validation exceptions.
 * Uses {@link HttpErrorCode} (ERR-HTTP-001..099).
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.HTTP_HANDLER)
public class HttpExceptionHandler extends AbstractExceptionHandler {

    public HttpExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        super(tracer, request, properties);
    }

    public HttpExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties, PayloadMasker payloadMasker) {
        super(tracer, request, properties, payloadMasker);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(HttpErrorCode.METHOD_NOT_ALLOWED, ex, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return buildErrorResponse(HttpErrorCode.UNSUPPORTED_MEDIA_TYPE, ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return buildErrorResponse(HttpErrorCode.NOT_ACCEPTABLE, ex, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMissingPathVariable(MissingPathVariableException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMissingPart(MissingServletRequestPartException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleRequestBinding(ServletRequestBindingException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpErrorCode.BAD_REQUEST, ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMessageNotWritable(HttpMessageNotWritableException ex) {
        return buildErrorResponse(HttpErrorCode.INTERNAL_SERVER_ERROR, ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConversionNotSupportedException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleConversionNotSupported(ConversionNotSupportedException ex) {
        return buildErrorResponse(HttpErrorCode.INTERNAL_SERVER_ERROR, ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return buildErrorResponse(HttpErrorCode.RESOURCE_NOT_FOUND, ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildErrorResponse(HttpErrorCode.RESOURCE_NOT_FOUND, ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleAsyncRequestTimeout(AsyncRequestTimeoutException ex) {
        return buildErrorResponse(HttpErrorCode.REQUEST_TIMEOUT, ex, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        details.put(ErrorDetailKeys.FIELD_ERRORS, fieldErrors);
        return buildErrorResponse(
                HttpErrorCode.VALIDATION_FAILED,
                "Validation failed for argument: " + ex.getObjectName(),
                details,
                HttpStatus.BAD_REQUEST,
                ex
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                HttpErrorCode.VALIDATION_FAILED,
                ex.getMessage(),
                details,
                HttpStatus.BAD_REQUEST,
                ex
        );
    }
}

