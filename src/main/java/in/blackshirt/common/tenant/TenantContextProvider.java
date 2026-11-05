package in.blackshirt.common.tenant;

import in.blackshirt.common.config.BlackshirtProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Automated Spring Bean used to populate, resolve, and propagate the {@link TenantContext} POJO across microservices.
 *
 * <h2>Usage in Microservice Inter-Communication</h2>
 * <pre>{@code
 * // 1. Inject the provider
 * @Autowired
 * private TenantContextProvider tenantContextProvider;
 *
 * // 2. Read context POJO
 * TenantContext context = tenantContextProvider.getTenantContext();
 * String tenantId = context.getTenantId();
 *
 * // 3. Pass headers to WebClient, RestClient, or RestTemplate
 * Map<String, String> tenantHeaders = tenantContextProvider.getHeadersMap();
 *
 * // Or directly propagate to Spring HttpHeaders:
 * HttpHeaders headers = new HttpHeaders();
 * tenantContextProvider.propagate(headers);
 * }</pre>
 */
@Component
public class TenantContextProvider {

    public static final String TENANT_ID_MDC_KEY = "tenantId";
    public static final String CALLER_SERVICE_MDC_KEY = "callerService";
    public static final String USER_ID_MDC_KEY = "userId";

    private final BlackshirtProperties properties;

    public TenantContextProvider(BlackshirtProperties properties) {
        this.properties = properties != null ? properties : new BlackshirtProperties();
    }

    /**
     * Automatically extracts tenant headers from an incoming {@link HttpServletRequest},
     * populates {@link TenantContextHolder}, and binds identifiers to SLF4J MDC.
     *
     * @param request the incoming servlet request
     * @return the populated {@link TenantContext} POJO
     * @throws MissingTenantException if tenantId is missing and {@code blackshirt.tenant.require-tenant-id} is true
     */
    public TenantContext fillFromRequest(HttpServletRequest request) {
        if (request == null) {
            clear();
            return TenantContext.EMPTY;
        }

        BlackshirtProperties.TenantProperties tenantProps = properties.getTenant();

        String tenantId = request.getHeader(tenantProps.getTenantHeaderName());
        if (tenantId != null) {
            tenantId = tenantId.trim();
        }

        if ((tenantId == null || tenantId.isEmpty()) && tenantProps.isRequireTenantId()) {
            throw new MissingTenantException(tenantProps.getTenantHeaderName());
        }

        String userId = request.getHeader(tenantProps.getUserHeaderName());
        if (userId != null) {
            userId = userId.trim();
            if (userId.isEmpty()) {
                userId = null;
            }
        }

        String callerService = request.getHeader(tenantProps.getServiceHeaderName());
        if (callerService == null || callerService.isBlank()) {
            callerService = request.getHeader("X-Caller-Service");
        }
        String currentService = (properties.getLog() != null && properties.getLog().getServiceName() != null)
                ? properties.getLog().getServiceName()
                : "unknown-service";
        String serviceName = (callerService != null && !callerService.isBlank()) ? callerService.trim() : currentService;

        String rawTimestamp = request.getHeader(tenantProps.getTimestampHeaderName());
        Instant timestamp = parseTimestamp(rawTimestamp);

        TenantContext context = new TenantContext(tenantId, serviceName, timestamp, userId);
        fill(context);
        return context;
    }

    /**
     * Binds the given {@link TenantContext} POJO to the thread context and updates SLF4J MDC.
     */
    public void fill(TenantContext context) {
        TenantContextHolder.set(context);

        if (properties.getTenant().isPropagateToMdc()) {
            BlackshirtProperties.TenantProperties tenantProps = properties.getTenant();
            if (context != null && context.getTenantId() != null) {
                MDC.put(tenantProps.getTenantMdcKey(), context.getTenantId());
            }
            if (context != null && context.getServiceName() != null && !context.getServiceName().isBlank()) {
                MDC.put(tenantProps.getServiceMdcKey(), context.getServiceName());
            }
            if (context != null && context.getUserId() != null && !context.getUserId().isBlank()) {
                MDC.put(tenantProps.getUserMdcKey(), context.getUserId());
            }
        }
    }

    /**
     * Convenience method to manually populate tenant context for background tasks or messaging.
     */
    public void fill(String tenantId, String userId) {
        String serviceName = (properties.getLog() != null && properties.getLog().getServiceName() != null)
                ? properties.getLog().getServiceName()
                : "unknown-service";
        fill(new TenantContext(tenantId, serviceName, userId));
    }

    /**
     * Retrieves the current {@link TenantContext} POJO.
     */
    public TenantContext getTenantContext() {
        return TenantContextHolder.get();
    }

    /**
     * Retrieves the current tenant ID.
     */
    public String getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * Retrieves the current user ID, if present.
     */
    public String getUserId() {
        return TenantContextHolder.getUserId();
    }

    /**
     * Retrieves the optional user ID.
     */
    public Optional<String> getOptionalUserId() {
        return TenantContextHolder.getOptionalUserId();
    }

    /**
     * Retrieves the current service name.
     */
    public String getServiceName() {
        return TenantContextHolder.getServiceName();
    }

    /**
     * Retrieves the current caller service name.
     */
    public String getCallerService() {
        return TenantContextHolder.getCallerService();
    }

    /**
     * Retrieves the context timestamp.
     */
    public Instant getTimestamp() {
        return TenantContextHolder.get().getTimestamp();
    }

    /**
     * Returns true if a non-blank tenant identifier is present in context.
     */
    public boolean isPresent() {
        return TenantContextHolder.get().isPresent();
    }

    /**
     * Generates a header map containing all tenant and correlation headers for outgoing
     * microservice HTTP/RPC calls.
     */
    public Map<String, String> getHeadersMap() {
        TenantContext context = TenantContextHolder.get();
        BlackshirtProperties.TenantProperties tenantProps = properties.getTenant();
        Map<String, String> headers = new LinkedHashMap<>();

        if (context.getTenantId() != null && !context.getTenantId().isBlank()) {
            headers.put(tenantProps.getTenantHeaderName(), context.getTenantId());
        }

        String outgoingServiceName = (properties.getLog() != null && properties.getLog().getServiceName() != null)
                ? properties.getLog().getServiceName()
                : (context.getServiceName() != null ? context.getServiceName() : "unknown-service");
        headers.put(tenantProps.getServiceHeaderName(), outgoingServiceName);

        headers.put(tenantProps.getTimestampHeaderName(), Instant.now().toString());

        if (context.getUserId() != null && !context.getUserId().isBlank()) {
            headers.put(tenantProps.getUserHeaderName(), context.getUserId());
        }

        return Collections.unmodifiableMap(headers);
    }

    /**
     * Injects tenant headers directly into Spring's {@link HttpHeaders} for outgoing REST calls.
     */
    public void propagate(HttpHeaders httpHeaders) {
        if (httpHeaders != null) {
            getHeadersMap().forEach(httpHeaders::set);
        }
    }

    /**
     * Clears the current {@link TenantContext} and removes tenant keys from SLF4J MDC.
     */
    public void clear() {
        TenantContextHolder.clear();
        BlackshirtProperties.TenantProperties tenantProps = properties.getTenant();
        try {
            MDC.remove(tenantProps.getTenantMdcKey());
            MDC.remove(tenantProps.getServiceMdcKey());
            MDC.remove(tenantProps.getUserMdcKey());
        } catch (Exception ignored) {
            // MDC may be unavailable or uninitialized
        }
    }

    private static Instant parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                long epochMillis = Long.parseLong(raw.trim());
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException e) {
                return Instant.now();
            }
        }
    }
}
