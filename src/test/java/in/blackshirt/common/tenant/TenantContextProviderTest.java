package in.blackshirt.common.tenant;

import in.blackshirt.common.config.BlackshirtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextProviderTest {

    private BlackshirtProperties properties;
    private TenantContextProvider provider;

    @BeforeEach
    void setUp() {
        properties = new BlackshirtProperties();
        properties.getLog().setServiceName("payment-service");
        provider = new TenantContextProvider(properties);
    }

    @AfterEach
    void tearDown() {
        provider.clear();
    }

    @Test
    @DisplayName("fillFromRequest extracts all headers, binds to context POJO and MDC")
    void testFillFromRequestComplete() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-alpha");
        request.addHeader("X-User-Id", "user-456");
        request.addHeader("X-Service-Name", "order-service");
        Instant customTimestamp = Instant.parse("2026-05-01T12:00:00Z");
        request.addHeader("X-Timestamp", customTimestamp.toString());

        TenantContext context = provider.fillFromRequest(request);

        assertEquals("tenant-alpha", context.getTenantId());
        assertEquals("user-456", context.getUserId());
        assertEquals("order-service", context.getServiceName());
        assertEquals(customTimestamp, context.getTimestamp());

        // Verify getter shortcuts
        assertEquals("tenant-alpha", provider.getTenantId());
        assertEquals("user-456", provider.getUserId());
        assertEquals("order-service", provider.getServiceName());
        assertTrue(provider.isPresent());

        // Verify MDC
        assertEquals("tenant-alpha", MDC.get("tenantId"));
        assertEquals("order-service", MDC.get("callerService"));
        assertEquals("user-456", MDC.get("userId"));
    }

    @Test
    @DisplayName("fillFromRequest uses local service name when caller service header is missing")
    void testFillFromRequestDefaultServiceName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-beta");

        TenantContext context = provider.fillFromRequest(request);

        assertEquals("tenant-beta", context.getTenantId());
        assertEquals("payment-service", context.getServiceName());
        assertNull(context.getUserId());
        assertNotNull(context.getTimestamp());
    }

    @Test
    @DisplayName("fillFromRequest throws MissingTenantException when requireTenantId is enabled")
    void testRequireTenantIdThrows() {
        properties.getTenant().setRequireTenantId(true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        MissingTenantException ex = assertThrows(MissingTenantException.class, () -> provider.fillFromRequest(request));
        assertEquals("ERR-HTTP-001", ex.getErrorCode().getCode());
        assertEquals(400, ex.getErrorCode().getHttpStatus());
    }

    @Test
    @DisplayName("fillFromRequest handles missing tenantId gracefully when requireTenantId is false")
    void testOptionalTenantAllowed() {
        properties.getTenant().setRequireTenantId(false);
        MockHttpServletRequest request = new MockHttpServletRequest();

        TenantContext context = provider.fillFromRequest(request);

        assertNotNull(context);
        assertFalse(context.isPresent());
        assertNull(context.getTenantId());
    }

    @Test
    @DisplayName("clear unbinds context and removes MDC keys")
    void testClear() {
        provider.fill("tenant-123", "user-789");
        assertEquals("tenant-123", MDC.get("tenantId"));
        assertEquals("user-789", MDC.get("userId"));

        provider.clear();

        assertFalse(provider.isPresent());
        assertNull(MDC.get("tenantId"));
        assertNull(MDC.get("callerService"));
        assertNull(MDC.get("userId"));
    }

    @Test
    @DisplayName("getHeadersMap generates correct headers for microservice inter-communication")
    void testGetHeadersMap() {
        provider.fill("tenant-acme", "user-888");

        Map<String, String> headers = provider.getHeadersMap();

        assertEquals("tenant-acme", headers.get("X-Tenant-Id"));
        assertEquals("payment-service", headers.get("X-Service-Name"));
        assertEquals("user-888", headers.get("X-User-Id"));
        assertNotNull(headers.get("X-Timestamp"));
    }

    @Test
    @DisplayName("propagate copies headers to Spring HttpHeaders")
    void testPropagateToHttpHeaders() {
        provider.fill("tenant-acme", "user-888");
        HttpHeaders httpHeaders = new HttpHeaders();

        provider.propagate(httpHeaders);

        assertEquals("tenant-acme", httpHeaders.getFirst("X-Tenant-Id"));
        assertEquals("payment-service", httpHeaders.getFirst("X-Service-Name"));
        assertEquals("user-888", httpHeaders.getFirst("X-User-Id"));
        assertNotNull(httpHeaders.getFirst("X-Timestamp"));
    }
}
