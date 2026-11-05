package in.blackshirt.common.tenant;

import in.blackshirt.common.config.BlackshirtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantClientRequestInterceptorTest {

    private BlackshirtProperties properties;
    private TenantContextProvider provider;
    private TenantClientRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new BlackshirtProperties();
        properties.getLog().setServiceName("checkout-service");
        provider = new TenantContextProvider(properties);
        interceptor = new TenantClientRequestInterceptor(provider);
    }

    @AfterEach
    void tearDown() {
        provider.clear();
    }

    @Test
    @DisplayName("Interceptor automatically attaches tenant headers to outgoing HTTP requests")
    void testOutgoingRequestHeaders() throws IOException {
        provider.fill("tenant-acme", "user-333");

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://payment-service/api/pay"));
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = (req, b) -> new MockClientHttpResponse();

        interceptor.intercept(request, body, execution);

        HttpHeaders headers = request.getHeaders();
        assertEquals("tenant-acme", headers.getFirst("X-Tenant-Id"));
        assertEquals("checkout-service", headers.getFirst("X-Service-Name"));
        assertEquals("user-333", headers.getFirst("X-User-Id"));
        assertNotNull(headers.getFirst("X-Timestamp"));
    }
}
