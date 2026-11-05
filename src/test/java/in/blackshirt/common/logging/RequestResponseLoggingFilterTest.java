package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.tenant.TenantContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestResponseLoggingFilterTest {

    private final BlackshirtProperties properties = new BlackshirtProperties();
    private final PayloadMasker masker = new JsonPayloadMasker(properties, null);
    private final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

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
    @DisplayName("Filter binds tenantId and callerService to MDC from headers and TenantContext")
    void testTenantAndCallerServiceMdcBinding() throws ServletException, IOException {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader("X-Tenant-Id", "tenant-alpha");
        request.addHeader("X-Service-Name", "checkout-service");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTenant = new AtomicReference<>();
        AtomicReference<String> capturedCaller = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedTenant.set(MDC.get(RequestResponseLoggingFilter.TENANT_ID_MDC_KEY));
            capturedCaller.set(MDC.get(RequestResponseLoggingFilter.CALLER_SERVICE_MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertEquals("tenant-alpha", capturedTenant.get());
        assertEquals("checkout-service", capturedCaller.get());
        assertNull(MDC.get(RequestResponseLoggingFilter.TENANT_ID_MDC_KEY));
        assertNull(MDC.get(RequestResponseLoggingFilter.CALLER_SERVICE_MDC_KEY));
    }

    @Test
    @DisplayName("Filter logs request and response with headers, query params, path variables, and body")
    void testLoggingWithHeadersParamsAndBody() throws ServletException, IOException {
        properties.getLogging().setIncludeHeaders(true);
        properties.getLogging().setIncludePayload(true);

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments");
        request.setQueryString("currency=USD&token=supersecret123");
        request.addHeader("Authorization", "Bearer sensitive-token");
        request.addHeader("X-Tenant-Id", "tenant-99");
        request.addHeader(RequestResponseLoggingFilter.CALLER_SERVICE_HEADER, "order-service");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"mysecret\",\"amount\":100}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("paymentId", "pay-123"));
            res.setContentType("application/json");
            res.getOutputStream().write("{\"status\":\"APPROVED\",\"token\":\"auth-token\"}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Filter populates MDC correlation coordinates from TenantContext")
    void testTenantContextMdcCorrelation() throws ServletException, IOException {
        properties.getLogging().setIncludeHeaders(true);
        properties.getLogging().setIncludePayload(true);

        TenantContext.set(TenantContext.of("tenant-abc", "cart-service", "user-100"));

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders/ORD-1/pay");
        request.setQueryString("notify=true");
        request.addHeader("X-Trace-Id", "json-test-trace-123");
        request.setContentType("application/json");
        request.setContent("{\"pin\":1234}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantInChain = new AtomicReference<>();
        AtomicReference<String> callerInChain = new AtomicReference<>();
        AtomicReference<String> userInChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            tenantInChain.set(MDC.get(RequestResponseLoggingFilter.TENANT_ID_MDC_KEY));
            callerInChain.set(MDC.get(RequestResponseLoggingFilter.CALLER_SERVICE_MDC_KEY));
            userInChain.set(MDC.get(RequestResponseLoggingFilter.USER_ID_MDC_KEY));

            req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("orderId", "ORD-1"));
            res.setContentType("application/json");
            res.getOutputStream().write("{\"status\":\"PAID\"}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("tenant-abc", tenantInChain.get());
        assertEquals("cart-service", callerInChain.get());
        assertEquals("user-100", userInChain.get());
        assertNull(MDC.get(RequestResponseLoggingFilter.TENANT_ID_MDC_KEY));
    }

    @Test
    @DisplayName("Filter logs exception and rethrows when unhandled error occurs in chain")
    void testExceptionThrownInFilterChain() {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(
                properties,
                masker,
                beanFactory.getBeanProvider(Tracer.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/faulty");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("Database connectivity lost");
        };

        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY));
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
