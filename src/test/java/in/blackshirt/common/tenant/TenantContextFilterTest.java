package in.blackshirt.common.tenant;

import in.blackshirt.common.config.BlackshirtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextFilterTest {

    private BlackshirtProperties properties;
    private TenantContextProvider provider;
    private TenantContextFilter filter;

    @BeforeEach
    void setUp() {
        properties = new BlackshirtProperties();
        properties.getLog().setServiceName("inventory-service");
        provider = new TenantContextProvider(properties);
        filter = new TenantContextFilter(provider, properties);
    }

    @AfterEach
    void tearDown() {
        provider.clear();
    }

    @Test
    @DisplayName("Filter populates context inside chain and clears it afterwards")
    void testFilterPopulatesAndCleansUp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/items");
        request.addHeader("X-Tenant-Id", "tenant-xyz");
        request.addHeader("X-User-Id", "user-555");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean insideChain = new AtomicBoolean(false);
        AtomicReference<String> capturedTenant = new AtomicReference<>();
        AtomicReference<String> capturedUser = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            insideChain.set(true);
            capturedTenant.set(TenantContextHolder.getTenantId());
            capturedUser.set(TenantContextHolder.getUserId());
        };

        filter.doFilter(request, response, chain);

        assertTrue(insideChain.get(), "Filter chain must execute");
        assertEquals("tenant-xyz", capturedTenant.get());
        assertEquals("user-555", capturedUser.get());

        // Context must be cleared after filter execution completes
        assertFalse(TenantContextHolder.get().isPresent());
    }

    @Test
    @DisplayName("Filter is bypassed when blackshirt.tenant.enabled is false")
    void testFilterDisabled() throws ServletException, IOException {
        properties.getTenant().setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/items");
        request.addHeader("X-Tenant-Id", "tenant-xyz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTenant = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTenant.set(TenantContextHolder.getTenantId());

        filter.doFilter(request, response, chain);

        assertFalse(TenantContextHolder.get().isPresent());
    }
}
