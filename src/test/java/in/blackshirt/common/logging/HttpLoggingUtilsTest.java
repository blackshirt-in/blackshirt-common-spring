package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpLoggingUtilsTest {

    private final BlackshirtProperties properties = new BlackshirtProperties();
    private final PayloadMasker masker = new JsonPayloadMasker(properties, null);

    @Test
    @DisplayName("extractClientIp resolves X-Forwarded-For first IP without regex splits")
    void testExtractClientIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");

        String ip = HttpLoggingUtils.extractClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("extractClientIp resolves single X-Forwarded-For IP")
    void testExtractClientIpSingleForwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String ip = HttpLoggingUtils.extractClientIp(request);
        assertEquals("198.51.100.1", ip);
    }

    @Test
    @DisplayName("extractClientIp falls back to X-Real-IP and remoteAddr")
    void testExtractClientIpFallback() {
        MockHttpServletRequest requestRealIp = new MockHttpServletRequest();
        requestRealIp.addHeader("X-Real-IP", "192.0.2.1");
        assertEquals("192.0.2.1", HttpLoggingUtils.extractClientIp(requestRealIp));

        MockHttpServletRequest requestRemote = new MockHttpServletRequest();
        requestRemote.setRemoteAddr("10.0.0.5");
        assertEquals("10.0.0.5", HttpLoggingUtils.extractClientIp(requestRemote));

        assertEquals("unknown", HttpLoggingUtils.extractClientIp(null));
    }

    @Test
    @DisplayName("appendHeaders writes masked headers directly to buffer")
    void testAppendHeadersDirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sensitive-jwt");
        request.addHeader("X-Custom-Trace", "trace-123");

        StringBuilder sb = new StringBuilder();
        HttpLoggingUtils.appendHeaders(sb, request, masker);

        String result = sb.toString();
        assertTrue(result.contains("Authorization=***"));
        assertTrue(result.contains("X-Custom-Trace=trace-123"));
        assertFalse(result.contains("sensitive-jwt"));
    }

    @Test
    @DisplayName("appendPathVariables appends map directly without defensive copies")
    void testAppendPathVariables() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("orderId", "ORD-999"));

        StringBuilder sb = new StringBuilder();
        HttpLoggingUtils.appendPathVariables(sb, request);

        assertEquals(" | pathVars={orderId=ORD-999}", sb.toString());
    }

    @Test
    @DisplayName("readCachedBody truncates gracefully and supports fast-path UTF-8")
    void testReadCachedBody() {
        byte[] bytes = "Hello World! Long Payload Example".getBytes(StandardCharsets.UTF_8);

        String full = HttpLoggingUtils.readCachedBody(bytes, "UTF-8", 100);
        assertEquals("Hello World! Long Payload Example", full);

        String truncated = HttpLoggingUtils.readCachedBody(bytes, "UTF-8", 5);
        assertEquals("Hello...[TRUNCATED]", truncated);

        assertEquals("", HttpLoggingUtils.readCachedBody(null, "UTF-8", 100));
        assertEquals("", HttpLoggingUtils.readCachedBody(new byte[0], "UTF-8", 100));
    }

    @Test
    @DisplayName("generateTraceId produces 32-character hex ID and generateSpanId produces 16-character hex ID")
    void testTraceAndSpanIdGeneration() {
        String traceId = HttpLoggingUtils.generateTraceId();
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("^[0-9a-f]{32}$"));

        String spanId = HttpLoggingUtils.generateSpanId();
        assertNotNull(spanId);
        assertEquals(16, spanId.length());
        assertTrue(spanId.matches("^[0-9a-f]{16}$"));
    }
}

