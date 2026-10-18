package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestResponseLoggingFilterTest {

    private final BlackshirtProperties properties = new BlackshirtProperties();
    private final PayloadMasker masker = new JsonPayloadMasker(properties, null);
    private final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    @Test
    @DisplayName("Filter generates traceId, sets MDC, sets response header, and clears MDC in finally")
    void testTraceIdGenerationAndMdcLifecycle() throws ServletException, IOException {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTraceIdInChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedTraceIdInChain.set(MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY));
            assertNotNull(capturedTraceIdInChain.get());
            assertEquals(capturedTraceIdInChain.get(), MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY));
            ((HttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertNotNull(capturedTraceIdInChain.get());
        assertEquals(capturedTraceIdInChain.get(), response.getHeader("X-Trace-Id"));
        // MDC must be completely cleared after filter execution
        assertNull(MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("Filter propagates incoming X-Trace-Id header")
    void testIncomingTraceIdPropagation() throws ServletException, IOException {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.addHeader("X-Trace-Id", "custom-client-trace-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTraceId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedTraceId.set(MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertEquals("custom-client-trace-999", capturedTraceId.get());
        assertEquals("custom-client-trace-999", response.getHeader("X-Trace-Id"));
    }

    @Test
    @DisplayName("Filter excludes configured paths such as /actuator/**")
    void testExcludedPaths() {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("GET", "/actuator/health");
        assertTrue(filter.shouldNotFilter(actuatorRequest));

        MockHttpServletRequest swaggerRequest = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        assertTrue(filter.shouldNotFilter(swaggerRequest));

        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/v1/users");
        assertFalse(filter.shouldNotFilter(apiRequest));
    }

    @Test
    @DisplayName("Filter copies body to response when includePayload=true")
    void testPayloadCachingAndCopyBody() throws ServletException, IOException {
        properties.getLogging().setIncludePayload(true);

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notifications");
        request.setContentType("application/json");
        request.setContent("{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            res.getOutputStream().write("{\"status\":\"SENT\"}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("{\"status\":\"SENT\"}", response.getContentAsString());
    }
}
