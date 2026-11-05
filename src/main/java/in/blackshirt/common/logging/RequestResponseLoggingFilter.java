package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
import in.blackshirt.common.tenant.TenantContextHolder;
import in.blackshirt.common.tenant.TenantContextProvider;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

/**
 * Perimeter servlet filter providing end-to-end request correlation, distributed tracing,
 * multi-tenant context propagation into SLF4J MDC, structured request/response logging,
 * payload and header masking, and exception tracking.
 *
 * <p>Correlation coordinates ({@code traceId}, {@code spanId}, {@code clientIp},
 * {@code tenantId}, {@code callerService}, {@code userId}) are placed in SLF4J MDC
 * and formatted uniformly by standard Spring Boot / Logback logging pattern properties
 * (e.g. {@code logging.pattern.console} or {@code logging.pattern.correlation}).
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String CALLER_SERVICE_HEADER = "X-Caller-Service";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";
    public static final String CLIENT_IP_MDC_KEY = "clientIp";
    public static final String TENANT_ID_MDC_KEY = TenantContextProvider.TENANT_ID_MDC_KEY;
    public static final String CALLER_SERVICE_MDC_KEY = TenantContextProvider.CALLER_SERVICE_MDC_KEY;
    public static final String USER_ID_MDC_KEY = TenantContextProvider.USER_ID_MDC_KEY;

    private final BlackshirtProperties properties;
    private final PayloadMasker payloadMasker;
    private final ObjectProvider<Tracer> tracerProvider;

    public RequestResponseLoggingFilter(BlackshirtProperties properties,
                                        PayloadMasker payloadMasker,
                                        ObjectProvider<Tracer> tracerProvider) {
        this.properties = properties;
        this.payloadMasker = payloadMasker;
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        BlackshirtProperties.Logging logging = properties.getLogging();
        if (logging == null || !logging.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        List<String> excludePaths = logging.getExcludePaths();
        if (excludePaths != null) {
            for (String pattern : excludePaths) {
                if (PATH_MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        setupTraceContext(request, response);

        BlackshirtProperties.Logging logging = properties.getLogging();
        boolean includePayload = logging.isIncludePayload();

        HttpServletRequest reqToUse = (includePayload && !request.isAsyncStarted())
                ? new ContentCachingRequestWrapper(request, logging.getMaxPayloadLength())
                : request;

        HttpServletResponse resToUse = (includePayload && !request.isAsyncStarted())
                ? new ContentCachingResponseWrapper(response)
                : response;

        logRequestIn(reqToUse, logging);

        try {
            filterChain.doFilter(reqToUse, resToUse);
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            logExceptionThrown(reqToUse, ex, duration, logging);
            throw ex;
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;
                logRequestOut(reqToUse, resToUse, duration, logging);

                if (resToUse instanceof ContentCachingResponseWrapper responseWrapper) {
                    responseWrapper.copyBodyToResponse();
                }
            } finally {
                MDC.clear();
            }
        }
    }

    private void setupTraceContext(HttpServletRequest request, HttpServletResponse response) {
        String traceId = "";
        String spanId = "";

        Tracer tracer = tracerProvider != null ? tracerProvider.getIfAvailable() : null;
        if (tracer != null) {
            var traceIds = TraceContext.current(tracer);
            traceId = traceIds.traceId();
            spanId = traceIds.spanId();
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(TRACE_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = HttpLoggingUtils.generateTraceId();
            }
        }

        if (spanId == null || spanId.isBlank()) {
            spanId = HttpLoggingUtils.generateSpanId();
        }

        String clientIp = HttpLoggingUtils.extractClientIp(request);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(SPAN_ID_MDC_KEY, spanId);
        MDC.put(CLIENT_IP_MDC_KEY, clientIp);

        // Ensure tenantId, callerService, and userId are bound to MDC
        String tenantId = TenantContextHolder.getTenantId();
        if ((tenantId == null || tenantId.isBlank()) && properties.getTenant() != null) {
            tenantId = request.getHeader(properties.getTenant().getTenantHeaderName());
        }
        if (tenantId != null && !tenantId.isBlank()) {
            MDC.put(TENANT_ID_MDC_KEY, tenantId);
        }

        String callerService = TenantContextHolder.getCallerService();
        if ((callerService == null || callerService.isBlank() || "unknown-service".equals(callerService)) && properties.getTenant() != null) {
            callerService = request.getHeader(properties.getTenant().getServiceHeaderName());
            if (callerService == null || callerService.isBlank()) {
                callerService = request.getHeader(CALLER_SERVICE_HEADER);
            }
        }
        if (callerService != null && !callerService.isBlank()) {
            MDC.put(CALLER_SERVICE_MDC_KEY, callerService);
        }

        String userId = TenantContextHolder.getUserId();
        if (userId != null && !userId.isBlank()) {
            MDC.put(USER_ID_MDC_KEY, userId);
        }

        response.setHeader(TRACE_HEADER, traceId);
    }

    private void logRequestIn(HttpServletRequest request, BlackshirtProperties.Logging logging) {
        if (!log.isInfoEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder(256);
        sb.append("--> ").append(request.getMethod()).append(' ').append(request.getRequestURI());

        String query = request.getQueryString();
        if (query != null && !query.isBlank() && logging.isIncludeQueryParams()) {
            sb.append('?').append(payloadMasker.maskPayload(query));
        }

        sb.append(" (client=").append(HttpLoggingUtils.extractClientIp(request)).append(')');

        if (logging.isIncludeHeaders()) {
            HttpLoggingUtils.appendHeaders(sb, request, payloadMasker);
        }

        if (logging.isIncludePayload() && request instanceof ContentCachingRequestWrapper reqWrapper) {
            String reqBody = HttpLoggingUtils.readCachedBody(reqWrapper.getContentAsByteArray(), reqWrapper.getCharacterEncoding(), logging.getMaxPayloadLength());
            if (!reqBody.isEmpty()) {
                sb.append(" | body=").append(payloadMasker.maskPayload(reqBody));
            }
        }

        log.info(sb.toString());
    }

    private void logRequestOut(HttpServletRequest request,
                               HttpServletResponse response,
                               long duration,
                               BlackshirtProperties.Logging logging) {
        if (!log.isInfoEnabled()) {
            return;
        }

        int status = response.getStatus();
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String reason = httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(status);

        StringBuilder sb = new StringBuilder(256);
        sb.append("<-- ").append(status).append(' ').append(reason)
                .append(' ').append(request.getMethod()).append(' ').append(request.getRequestURI())
                .append(" [").append(duration).append("ms]");

        if (logging.isIncludePathVariables()) {
            HttpLoggingUtils.appendPathVariables(sb, request);
        }

        if (logging.isIncludePayload()) {
            if (request instanceof ContentCachingRequestWrapper reqWrapper) {
                String reqBody = HttpLoggingUtils.readCachedBody(reqWrapper.getContentAsByteArray(), reqWrapper.getCharacterEncoding(), logging.getMaxPayloadLength());
                if (!reqBody.isEmpty()) {
                    sb.append(" | reqBody=").append(payloadMasker.maskPayload(reqBody));
                }
            }

            if (response instanceof ContentCachingResponseWrapper resWrapper) {
                String resBody = HttpLoggingUtils.readCachedBody(resWrapper.getContentAsByteArray(), resWrapper.getCharacterEncoding(), logging.getMaxPayloadLength());
                if (!resBody.isEmpty()) {
                    sb.append(" | resBody=").append(payloadMasker.maskPayload(resBody));
                }
            }
        }

        log.info(sb.toString());
    }

    private void logExceptionThrown(HttpServletRequest request, Throwable ex, long duration, BlackshirtProperties.Logging logging) {
        log.error("XX- {} {} [{}ms] Exception: {}: {}",
                request.getMethod(), request.getRequestURI(), duration,
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
    }
}
