package in.blackshirt.common.exception.handler;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.exception.ErrorDetailKeys;
import in.blackshirt.common.exception.code.HttpErrorCode;
import in.blackshirt.common.response.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpExceptionHandlerTest {

    private HttpExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/resource");
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLog().setServiceName("order-service");
        handler = new HttpExceptionHandler(Tracer.NOOP, request, properties);
    }

    @Test
    @DisplayName("Handles HttpRequestMethodNotSupportedException returning 405")
    void testMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET", "PUT"));

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleMethodNotSupported(ex);

        assertEquals(405, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpErrorCode.METHOD_NOT_ALLOWED.getCode(), response.getBody().errorCode());
        assertEquals("ERR-HTTP-006", response.getBody().errorCode());
        assertEquals("order-service", response.getBody().serviceName());
    }

    @Test
    @DisplayName("Handles MissingServletRequestParameterException returning 400")
    void testMissingParameter() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("page", "int");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleMissingParameter(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpErrorCode.BAD_REQUEST.getCode(), response.getBody().errorCode());
        assertEquals("ERR-HTTP-001", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handles NoResourceFoundException returning 404")
    void testNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown", "Resource not found");

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleNoResourceFound(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpErrorCode.RESOURCE_NOT_FOUND.getCode(), response.getBody().errorCode());
        assertEquals("ERR-HTTP-005", response.getBody().errorCode());
    }

    private void sampleControllerMethod(String param) {}

    @Test
    @DisplayName("Handles MethodArgumentNotValidException extracting field errors map")
    void testMethodArgumentNotValid() throws NoSuchMethodException {
        record DummyDto(String name) {}
        DummyDto target = new DummyDto("");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyDto");
        bindingResult.addError(new FieldError("dummyDto", "name", "must not be blank"));

        java.lang.reflect.Method method = getClass().getDeclaredMethod("sampleControllerMethod", String.class);
        org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse<Object>> response = handler.handleMethodArgumentNotValid(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpErrorCode.VALIDATION_FAILED.getCode(), response.getBody().errorCode());
        assertEquals("ERR-HTTP-002", response.getBody().errorCode());

        Map<String, Object> details = response.getBody().details();
        assertNotNull(details);
        assertTrue(details.containsKey(ErrorDetailKeys.FIELD_ERRORS));

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) details.get(ErrorDetailKeys.FIELD_ERRORS);
        assertEquals("must not be blank", fieldErrors.get("name"));
    }
}

