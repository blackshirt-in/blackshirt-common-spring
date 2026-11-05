package in.blackshirt.common.tenant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Client interceptor that automatically attaches tenant headers from {@link TenantContextProvider}
 * to outgoing HTTP calls made via {@code RestTemplate} or {@code RestClient}.
 */
public class TenantClientRequestInterceptor implements ClientHttpRequestInterceptor {

    private final TenantContextProvider tenantContextProvider;

    public TenantClientRequestInterceptor(TenantContextProvider tenantContextProvider) {
        this.tenantContextProvider = tenantContextProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        tenantContextProvider.propagate(request.getHeaders());
        return execution.execute(request, body);
    }
}
