package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.code.DatabaseErrorCode;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseExceptionHandlerTest {

    private DatabaseExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/v1/orders");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("payment-service");
        handler = new DatabaseExceptionHandler(Tracer.NOOP, request, properties);
    }

    @Test
    @DisplayName("Handles DuplicateKeyException returning 409 Conflict with sanitized message")
    void testDuplicateKey() {
        DuplicateKeyException ex = new DuplicateKeyException("Key (id)=(123) already exists");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleDuplicateKey(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.DUPLICATE_KEY.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-003", response.getBody().errorCode());
        assertTrue(response.getBody().errorMessage().contains("duplicate"));
    }

    @Test
    @DisplayName("Handles DataIntegrityViolationException returning 409 Conflict")
    void testDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violates foreign key constraint");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.DATA_INTEGRITY_VIOLATION.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-002", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles CannotAcquireLockException returning 409 Conflict")
    void testLockingFailure() {
        CannotAcquireLockException ex = new CannotAcquireLockException("could not obtain lock");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleLockingFailure(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.LOCK_FAILURE.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-004", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles QueryTimeoutException returning 504 Gateway Timeout")
    void testQueryTimeout() {
        QueryTimeoutException ex = new QueryTimeoutException("query timeout reached");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleQueryTimeout(ex);

        assertEquals(504, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.QUERY_TIMEOUT.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-005", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles generic DataAccessException returning 500 Database Error")
    void testGenericDataAccessException() {
        DataAccessException ex = new DataAccessException("connection pool exhausted") {};

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleDataAccessException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.DATABASE_ERROR.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-001", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles standard SQLException returning 500 Database Error")
    void testStandardSQLException() {
        SQLException ex = new SQLException("Connection refused: 5432");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleSQLException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(DatabaseErrorCode.DATABASE_ERROR.getCode(), response.getBody().errorCode());
        assertEquals("ERR-DB-001", response.getBody().errorCode());
    }
}

