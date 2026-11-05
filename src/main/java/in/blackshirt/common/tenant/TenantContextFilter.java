package in.blackshirt.common.tenant;

import in.blackshirt.common.config.BlackshirtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Perimeter filter that extracts incoming tenant headers into {@link TenantContext}
 * for the duration of the web request, guaranteeing clean unbinding in a {@code finally} block.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantContextProvider tenantContextProvider;
    private final BlackshirtProperties properties;

    public TenantContextFilter(TenantContextProvider tenantContextProvider, BlackshirtProperties properties) {
        this.tenantContextProvider = tenantContextProvider;
        this.properties = properties != null ? properties : new BlackshirtProperties();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        BlackshirtProperties.TenantProperties tenantProps = properties.getTenant();
        return tenantProps == null || !tenantProps.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            tenantContextProvider.fillFromRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            tenantContextProvider.clear();
        }
    }
}
