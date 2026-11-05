package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.ErrorDetailKeys;
import in.blackshirt.common.exception.code.SystemErrorCode;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackExceptionHandlerTest {

    private FallbackExceptionHandler handler;
    private MockHttpServletRequest request;
    private BlackshirtProperties properties;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/unknown");
        properties = new BlackshirtProperties();
        properties.getLog().setServiceName("core-service");
        handler = new FallbackExceptionHandler(Tracer.NOOP, request, properties);
    }

    @Test
    @DisplayName("Handles generic unhandled Exception returning 500")
    void testHandleUnexpected() {
        Exception ex = new RuntimeException("Unexpected IO socket glitch");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleUnexpected(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(SystemErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().errorCode());
        assertEquals("ERR-SYS-001", response.getBody().errorCode());
        assertEquals("Unexpected IO socket glitch", response.getBody().errorMessage());
        assertNotNull(response.getBody().details());
        assertTrue(response.getBody().details().containsKey(ErrorDetailKeys.STACKTRACE));
    }

    @Test
    @DisplayName("Omit stack trace when includeStackTrace is set to false")
    void testOmitStackTraceWhenDisabled() {
        properties.getExceptionHandler().setIncludeStackTrace(false);
        Exception ex = new RuntimeException("Sensitive internal crash");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleUnexpected(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().details());
        assertFalse(response.getBody().details().containsKey(ErrorDetailKeys.STACKTRACE),
                "Stacktrace should not be present when includeStackTrace is false");
    }

    @Test
    @DisplayName("Handles generic Throwable returning 500")
    void testHandleThrowable() {
        Throwable ex = new AssertionError("Internal assertion failed");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleThrowable(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(SystemErrorCode.CRITICAL_FAILURE.getCode(), response.getBody().errorCode());
        assertEquals("ERR-SYS-002", response.getBody().errorCode());
    }
}

