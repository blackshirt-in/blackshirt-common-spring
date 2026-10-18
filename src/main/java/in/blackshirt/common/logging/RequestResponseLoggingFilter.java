package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
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
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Perimeter servlet filter providing request correlation via SLF4J MDC,
 * clean non-duplicative request-in and request-out logging, duration tracking,
 * and property-driven payload and header masking.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";
    public static final String CLIENT_IP_MDC_KEY = "clientIp";

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
        boolean isLoggableReq = includePayload && ContentTypeGuard.isLoggable(request.getContentType()) && !request.isAsyncStarted();

        HttpServletRequest reqToUse = isLoggableReq
                ? new ContentCachingRequestWrapper(request, logging.getMaxPayloadLength())
                : request;

        HttpServletResponse resToUse = includePayload && !request.isAsyncStarted()
                ? new ContentCachingResponseWrapper(response)
                : response;

        logRequestIn(reqToUse, logging);

        try {
            filterChain.doFilter(reqToUse, resToUse);
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
        String traceHeaderName = properties.getLogging().getTraceHeaderName();
        if (traceHeaderName == null || traceHeaderName.isBlank()) {
            traceHeaderName = "X-Trace-Id";
        }

        String traceId = "";
        String spanId = "";

        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            var traceIds = TraceContext.current(tracer);
            traceId = traceIds.traceId();
            spanId = traceIds.spanId();
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(traceHeaderName);
            if (traceId == null || traceId.isBlank()) {
                traceId = request.getHeader("X-Correlation-ID");
            }
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
        }

        if (spanId == null || spanId.isBlank()) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String clientIp = extractClientIp(request);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(SPAN_ID_MDC_KEY, spanId);
        MDC.put(CLIENT_IP_MDC_KEY, clientIp);

        response.setHeader(traceHeaderName, traceId);
    }

    private void logRequestIn(HttpServletRequest request, BlackshirtProperties.Logging logging) {
        StringBuilder sb = new StringBuilder();
        sb.append("--> ").append(request.getMethod()).append(" ").append(request.getRequestURI());

        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            sb.append("?").append(payloadMasker.maskPayload(query));
        }

        sb.append(" (client=").append(extractClientIp(request)).append(")");

        if (logging.isIncludeHeaders()) {
            Map<String, String> headers = extractHeaders(request);
            Map<String, String> maskedHeaders = payloadMasker.maskHeaders(headers);
            sb.append(" | headers=").append(maskedHeaders);
        }

        log.info(sb.toString());
    }

    private void logRequestOut(HttpServletRequest request,
                               HttpServletResponse response,
                               long duration,
                               BlackshirtProperties.Logging logging) {
        int status = response.getStatus();
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String reason = httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(status);

        StringBuilder sb = new StringBuilder();
        sb.append("<-- ").append(status).append(" ").append(reason)
                .append(" ").append(request.getMethod()).append(" ").append(request.getRequestURI())
                .append(" [").append(duration).append("ms]");

        if (logging.isIncludePayload()) {
            if (request instanceof ContentCachingRequestWrapper reqWrapper) {
                String reqBody = getCachedBody(reqWrapper.getContentAsByteArray(), reqWrapper.getCharacterEncoding(), logging.getMaxPayloadLength());
                if (!reqBody.isBlank()) {
                    sb.append(" | ReqBody: ").append(payloadMasker.maskPayload(reqBody));
                }
            }

            if (response instanceof ContentCachingResponseWrapper resWrapper) {
                String resContentType = resWrapper.getContentType();
                if (ContentTypeGuard.isLoggable(resContentType)) {
                    String resBody = getCachedBody(resWrapper.getContentAsByteArray(), resWrapper.getCharacterEncoding(), logging.getMaxPayloadLength());
                    if (!resBody.isBlank()) {
                        sb.append(" | ResBody: ").append(payloadMasker.maskPayload(resBody));
                    }
                }
            }
        }

        log.info(sb.toString());
    }

    private String getCachedBody(byte[] content, String characterEncoding, int maxLength) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            int length = Math.min(content.length, maxLength);
            String charsetName = (characterEncoding != null && !characterEncoding.isBlank())
                    ? characterEncoding
                    : StandardCharsets.UTF_8.name();
            String body = new String(content, 0, length, charsetName);
            if (content.length > maxLength) {
                body += "...[TRUNCATED]";
            }
            return body;
        } catch (Exception e) {
            return "[BINARY/UNPARSEABLE]";
        }
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
