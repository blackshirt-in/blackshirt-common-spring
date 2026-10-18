package in.blackshirt.common.exception;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.response.ApiErrorResponse;
import in.blackshirt.common.response.ErrorCode;
import in.blackshirt.common.response.StandardErrorCode;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerLoggingTest {

    private static class CustomBusinessException extends BaseException {
        public CustomBusinessException(String message) {
            super(new ErrorCode() {
                @Override public String getCode() { return "TEST_400"; }
                @Override public String getDefaultMessage() { return "Bad Request"; }
                @Override public int getHttpStatus() { return 400; }
            }, message);
        }
    }

    private static class CustomServerException extends BaseException {
        public CustomServerException(String message) {
            super(new ErrorCode() {
                @Override public String getCode() { return "TEST_500"; }
                @Override public String getDefaultMessage() { return "Internal Failure"; }
                @Override public int getHttpStatus() { return 500; }
            }, message);
        }
    }

    @Test
    @DisplayName("handleBaseException handles 4xx client errors cleanly")
    void testHandleBaseException4xx() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("test-service");

        GlobalExceptionHandler handler = new GlobalExceptionHandler(Tracer.NOOP, request, properties);

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleBaseException(new CustomBusinessException("Invalid field"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("TEST_400", response.getBody().errorCode());
        assertEquals("Invalid field", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("handleBaseException handles 5xx server errors cleanly")
    void testHandleBaseException5xx() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("test-service");

        GlobalExceptionHandler handler = new GlobalExceptionHandler(Tracer.NOOP, request, properties);

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleBaseException(new CustomServerException("Database failure"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("TEST_500", response.getBody().errorCode());
        assertEquals("Database failure", response.getBody().errorMessage());
    }
}
