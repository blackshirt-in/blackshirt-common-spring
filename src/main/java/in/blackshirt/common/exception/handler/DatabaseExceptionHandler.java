package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.code.DatabaseErrorCode;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

/**
 * 2nd in the common exception handling pipeline.
 * Handles SQL, JDBC, and Spring DAO persistence exceptions.
 * Uses {@link DatabaseErrorCode} (ERR-DB-001..099).
 * Sanitizes sensitive internal database schema messages from direct exposure.
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.DATABASE_HANDLER)
public class DatabaseExceptionHandler extends AbstractExceptionHandler {

    public DatabaseExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties) {
        super(tracer, request, properties);
    }

    public DatabaseExceptionHandler(Tracer tracer, HttpServletRequest request, BlackshirtProperties properties, PayloadMasker payloadMasker) {
        super(tracer, request, properties, payloadMasker);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleDuplicateKey(DuplicateKeyException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.DUPLICATE_KEY,
                DatabaseErrorCode.DUPLICATE_KEY.getDefaultMessage(),
                details,
                HttpStatus.CONFLICT,
                ex
        );
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public <T> ResponseEntity<ApiErrorResponse<T>> handleDataIntegrityViolation(Exception ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.DATA_INTEGRITY_VIOLATION,
                DatabaseErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage(),
                details,
                HttpStatus.CONFLICT,
                ex
        );
    }

    @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockingFailureException.class})
    public <T> ResponseEntity<ApiErrorResponse<T>> handleLockingFailure(Exception ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.LOCK_FAILURE,
                DatabaseErrorCode.LOCK_FAILURE.getDefaultMessage(),
                details,
                HttpStatus.CONFLICT,
                ex
        );
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleQueryTimeout(QueryTimeoutException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.QUERY_TIMEOUT,
                DatabaseErrorCode.QUERY_TIMEOUT.getDefaultMessage(),
                details,
                HttpStatus.GATEWAY_TIMEOUT,
                ex
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleDataAccessException(DataAccessException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.DATABASE_ERROR,
                DatabaseErrorCode.DATABASE_ERROR.getDefaultMessage(),
                details,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
    }

    @ExceptionHandler(SQLException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> handleSQLException(SQLException ex) {
        Map<String, Object> details = buildBaseErrorDetails(ex);
        return buildErrorResponse(
                DatabaseErrorCode.DATABASE_ERROR,
                DatabaseErrorCode.DATABASE_ERROR.getDefaultMessage(),
                details,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
    }
}

