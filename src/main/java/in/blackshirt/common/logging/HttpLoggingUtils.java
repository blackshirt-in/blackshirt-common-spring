package in.blackshirt.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * High-performance, memory-optimized utility for HTTP perimeter logging,
 * header extraction, path variable formatting, and trace correlation ID generation.
 *
 * <p>Optimizations:
 * <ul>
 *   <li>Avoids regex compilation and array allocations in client IP extraction.</li>
 *   <li>Streams headers directly into {@link StringBuilder} without intermediate Map allocations.</li>
 *   <li>Avoids defensive map copies for Spring MVC path variables.</li>
 *   <li>Fast-path charset resolution avoiding Charset.forName cache lookups.</li>
 *   <li>Generates W3C-compliant 32-hex trace and 16-hex span IDs without UUID string churn.</li>
 * </ul>
 * </p>
 */
public final class HttpLoggingUtils {

    private static final String UNKNOWN = "unknown";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HttpLoggingUtils() {}

    /**
     * Extracts the client IP address without regex splits or array allocations.
     * Evaluates X-Forwarded-For (extracting the first proxy IP), X-Real-IP, and remote address.
     *
     * @param request the HTTP servlet request
     * @return the extracted client IP or "unknown"
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            int commaIdx = xForwardedFor.indexOf(',');
            return (commaIdx != -1 ? xForwardedFor.substring(0, commaIdx) : xForwardedFor).trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : UNKNOWN;
    }

    /**
     * Formats and appends HTTP headers directly into the provided {@link StringBuilder}
     * with masking, avoiding intermediate {@code Map<String, String>} allocations.
     *
     * @param sb the target StringBuilder buffer
     * @param request the incoming HTTP request
     * @param payloadMasker the masking provider
     */
    public static void appendHeaders(StringBuilder sb, HttpServletRequest request, PayloadMasker payloadMasker) {
        if (sb == null || request == null) {
            return;
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null || !headerNames.hasMoreElements()) {
            return;
        }

        sb.append(" | headers={");
        boolean first = true;
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (name == null) {
                continue;
            }

            String value = request.getHeader(name);
            if (!first) {
                sb.append(", ");
            }
            sb.append(name).append('=');

            if (payloadMasker != null) {
                sb.append(payloadMasker.maskHeaderValue(name, value));
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append('}');
    }

    /**
     * Extracts request headers into a new map (used when a physical map is strictly needed).
     *
     * @param request the HTTP request
     * @return header name-value map
     */
    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Enumeration<String> names = request.getHeaderNames();
        if (names == null || !names.hasMoreElements()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name != null) {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }

    /**
     * Appends Spring MVC path variables directly into the target {@link StringBuilder}
     * without creating defensive map copies.
     *
     * @param sb the target StringBuilder buffer
     * @param request the HTTP request containing template variables
     */
    public static void appendPathVariables(StringBuilder sb, HttpServletRequest request) {
        if (sb == null || request == null) {
            return;
        }
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attr instanceof Map<?, ?> map && !map.isEmpty()) {
            sb.append(" | pathVars=").append(map);
        }
    }

    /**
     * Extracts URI template variables without defensive copy allocations.
     *
     * @param request the HTTP request
     * @return map of path variables or empty map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> extractPathVariables(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attr instanceof Map<?, ?> map && !map.isEmpty()) {
            return (Map<String, String>) map;
        }
        return Collections.emptyMap();
    }

    /**
     * Decodes and bounds cached byte content using fast-path UTF-8 resolution.
     *
     * @param content cached body byte array
     * @param characterEncoding request/response character encoding
     * @param maxLength maximum characters to decode before truncating
     * @return decoded string or truncated representation
     */
    public static String readCachedBody(byte[] content, String characterEncoding, int maxLength) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            int length = Math.min(content.length, maxLength);
            Charset charset = resolveCharset(characterEncoding);
            String body = new String(content, 0, length, charset);
            if (content.length > maxLength) {
                return body + "...[TRUNCATED]";
            }
            return body;
        } catch (Exception e) {
            return "[BINARY/UNPARSEABLE]";
        }
    }

    /**
     * Fast-path charset resolution avoiding Charset.forName cache lookups for common encodings.
     *
     * @param characterEncoding encoding name
     * @return resolved Charset (defaults to UTF-8)
     */
    public static Charset resolveCharset(String characterEncoding) {
        if (characterEncoding == null || characterEncoding.isBlank()
                || "UTF-8".equalsIgnoreCase(characterEncoding)
                || "UTF8".equalsIgnoreCase(characterEncoding)) {
            return StandardCharsets.UTF_8;
        }
        if ("ISO-8859-1".equalsIgnoreCase(characterEncoding)) {
            return StandardCharsets.ISO_8859_1;
        }
        if ("US-ASCII".equalsIgnoreCase(characterEncoding)) {
            return StandardCharsets.US_ASCII;
        }
        try {
            return Charset.forName(characterEncoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Generates a 32-character lowercase hex trace ID conforming to W3C TraceContext
     * without UUID string allocation or hyphen replacement.
     *
     * @return 32-character hex trace ID
     */
    public static String generateTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] chars = new char[32];
        writeLongHex(chars, 0, random.nextLong());
        writeLongHex(chars, 16, random.nextLong());
        return new String(chars);
    }

    /**
     * Generates a 16-character lowercase hex span ID conforming to W3C TraceContext
     * without UUID string allocation.
     *
     * @return 16-character hex span ID
     */
    public static String generateSpanId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] chars = new char[16];
        writeLongHex(chars, 0, random.nextLong());
        return new String(chars);
    }

    private static void writeLongHex(char[] chars, int offset, long value) {
        for (int i = 15; i >= 0; i--) {
            chars[offset + i] = HEX_CHARS[(int) (value & 0xF)];
            value >>>= 4;
        }
    }
}

