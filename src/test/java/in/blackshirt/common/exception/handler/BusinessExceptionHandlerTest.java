package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.BaseException;
import in.blackshirt.common.exception.code.ErrorCode;
import in.blackshirt.common.exception.code.HttpErrorCode;
import in.blackshirt.common.response.ApiErrorResponse;
import in.blackshirt.common.tenant.TenantContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessExceptionHandlerTest {

    private BusinessExceptionHandler handler;
    private MockHttpServletRequest request;

    private static class OrderNotFoundException extends BaseException {
        public OrderNotFoundException(String orderId) {
            super(new ErrorCode() {
                @Override public String getCode() { return "ORDER_404"; }
                @Override public String getDefaultMessage() { return "Order not found"; }
                @Override public int getHttpStatus() { return 404; }
            }, "Order with ID " + orderId + " was not found");
            addDetails("orderId", orderId);
        }
    }

    private static class CustomServerException extends BaseException {
        public CustomServerException(String message) {
            super(new ErrorCode() {
                @Override public String getCode() { return "SERVER_500"; }
                @Override public String getDefaultMessage() { return "Internal Failure"; }
                @Override public int getHttpStatus() { return 500; }
            }, message);
        }
    }

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/orders/123");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("order-service");
        handler = new BusinessExceptionHandler(Tracer.NOOP, request, properties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Handles custom BaseException and propagates custom details and HTTP status")
    void testHandleBaseException() {
        OrderNotFoundException ex = new OrderNotFoundException("ord-999");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleBaseException(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("ORDER_404", response.getBody().errorCode());
        assertEquals("Order with ID ord-999 was not found", response.getBody().errorMessage());
        assertNotNull(response.getBody().details());
        assertEquals("ord-999", response.getBody().details().get("orderId"));
    }

    @Test
    @DisplayName("Handles 5xx server BaseException correctly")
    void testHandleBaseException5xx() {
        CustomServerException ex = new CustomServerException("Downstream core failure");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleBaseException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SERVER_500", response.getBody().errorCode());
        assertEquals("Downstream core failure", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("Handles Spring ResponseStatusException extracting reason and status")
    void testHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to account is restricted");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleResponseStatusException(ex);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpErrorCode.FORBIDDEN.getCode(), response.getBody().errorCode());
        assertEquals("Access to account is restricted", response.getBody().errorMessage());
    }

    @Test
    @DisplayName("Microservice exception automatically captures tenantId, callerService, userId, pathVariables, and masks sensitive params")
    @SuppressWarnings("unchecked")
    void testMicroserviceExceptionWithTenantAndPathVariablesAutoConfigured() {
        TenantContext.set(TenantContext.of("tenant-t1", "payment-service", "user-u1"));

        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("orderId", "123"));
        request.setParameter("page", "2");
        request.setParameter("token", "supersecret123");
        request.addHeader("Authorization", "Bearer sensitive-auth-token");
        request.addHeader("X-Custom-Header", "standard-value");

        OrderNotFoundException ex = new OrderNotFoundException("123");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleBaseException(ex);

        assertEquals(404, response.getStatusCode().value());
        Map<String, Object> details = response.getBody().details();
        assertNotNull(details);

        assertEquals("tenant-t1", details.get("tenantId"));
        assertEquals("payment-service", details.get("callerService"));
        assertEquals("user-u1", details.get("userId"));

        Map<String, String> pathVars = (Map<String, String>) details.get("pathVariables");
        assertNotNull(pathVars);
        assertEquals("123", pathVars.get("orderId"));

        Map<String, String> queryParams = (Map<String, String>) details.get("queryParams");
        assertNotNull(queryParams);
        assertEquals("2", queryParams.get("page"));
        assertEquals("***", queryParams.get("token")); // token must be masked!

        Map<String, String> headers = (Map<String, String>) details.get("requestHeaders");
        assertNotNull(headers);
        assertEquals("***", headers.get("Authorization")); // authorization must be masked!
        assertEquals("standard-value", headers.get("X-Custom-Header"));
    }
}
