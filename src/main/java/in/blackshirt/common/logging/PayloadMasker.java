package in.blackshirt.common.logging;

import java.util.Map;

/**
 * Interface for masking sensitive information in HTTP payloads and headers
 * before writing to log sinks.
 */
public interface PayloadMasker {

    /**
     * Masks sensitive fields in the given payload string.
     *
     * @param payload the raw request or response payload body
     * @return the masked payload, or the original payload if masking is disabled or empty
     */
    String maskPayload(String payload);

    /**
     * Masks sensitive HTTP headers (such as Authorization, Cookie, API keys).
     *
     * @param headers the map of header names and values
     * @return a new map containing masked sensitive header values
     */
    Map<String, String> maskHeaders(Map<String, String> headers);

    /**
     * Masks an individual header value if its name matches a sensitive header.
     *
     * @param name header name
     * @param value raw header value
     * @return masked value or original value
     */
    default String maskHeaderValue(String name, String value) {
        return value;
    }
}
