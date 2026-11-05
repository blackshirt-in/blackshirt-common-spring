package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.code.RuntimeErrorCode;
import in.blackshirt.common.response.ApiErrorResponse;
import in.blackshirt.common.tenant.TenantContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaRuntimeExceptionHandlerTest {

    private JavaRuntimeExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/v1/calculate");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("calc-service");
        handler = new JavaRuntimeExceptionHandler(Tracer.NOOP, request, properties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Handles NullPointerException returning 500 without leaking raw details in message")
    void testHandleNullPointer() {
        NullPointerException ex = new NullPointerException("Cannot invoke method on null object");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleNullPointer(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.NULL_POINTER.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-001", response.getBody().errorCode());
        assertTrue(response.getBody().errorMessage().contains("null reference"));
    }

    @Test
    @DisplayName("Handles IllegalArgumentException returning 400 Bad Request")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Quantity must be greater than zero");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.ILLEGAL_ARGUMENT.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-002", response.getBody().errorCode());
        assertEquals("Quantity must be greater than zero", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("Handles IllegalStateException returning 409 Conflict")
    void testHandleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Account is in suspended state");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleIllegalState(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.ILLEGAL_STATE.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-003", response.getBody().errorCode());
        assertEquals("Account is in suspended state", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("Handles ArithmeticException returning 400 Bad Request")
    void testHandleArithmetic() {
        ArithmeticException ex = new ArithmeticException("/ by zero");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleArithmetic(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.ARITHMETIC_ERROR.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-004", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles IndexOutOfBoundsException returning 400 Bad Request")
    void testHandleIndexOutOfBounds() {
        IndexOutOfBoundsException ex = new IndexOutOfBoundsException("Index 10 out of bounds for length 5");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleIndexOutOfBounds(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.INDEX_OUT_OF_BOUNDS.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-005", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles UnsupportedOperationException returning 501 Not Implemented")
    void testHandleUnsupportedOperation() {
        UnsupportedOperationException ex = new UnsupportedOperationException("Export to PDF is not yet supported");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleUnsupportedOperation(ex);

        assertEquals(501, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(RuntimeErrorCode.UNSUPPORTED_OPERATION.getCode(), response.getBody().errorCode());
        assertEquals("ERR-RT-006", response.getBody().errorCode());
        assertEquals("Export to PDF is not yet supported", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("Throws standard Java exception in microservice, auto-enriches tenantId and callerService in response details")
    void testMicroserviceThrowsJavaExceptionWithTenantContext() {
        TenantContext.set(TenantContext.of("tenant-finance", "billing-service", "admin-1"));

        IllegalArgumentException ex = new IllegalArgumentException("Tax rate cannot be negative");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> details = response.getBody().details();
        assertNotNull(details);
        assertEquals("tenant-finance", details.get("tenantId"));
        assertEquals("billing-service", details.get("callerService"));
        assertEquals("admin-1", details.get("userId"));
    }
}
