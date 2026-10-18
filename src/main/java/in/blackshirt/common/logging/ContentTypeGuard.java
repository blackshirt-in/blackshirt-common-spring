package in.blackshirt.common.logging;

import org.springframework.http.MediaType;

import java.util.Set;

/**
 * Utility guard to determine if an HTTP request or response Content-Type is safe
 * for payload caching and text logging.
 *
 * <p>Ensures streaming endpoints, multipart file uploads, and binary streams
 * are not buffered into memory.</p>
 */
public final class ContentTypeGuard {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    );

    private ContentTypeGuard() {}

    /**
     * Checks if the given Content-Type header value represents a loggable text/JSON/XML payload.
     *
     * @param contentType the raw Content-Type string (e.g. "application/json;charset=UTF-8")
     * @return {@code true} if the payload can be safely cached and logged, {@code false} otherwise
     */
    public static boolean isLoggable(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String cleanType = contentType.split(";")[0].trim().toLowerCase();

        // Reject binary/streaming types immediately
        if (cleanType.startsWith("multipart/") ||
                cleanType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE) ||
                cleanType.equals(MediaType.TEXT_EVENT_STREAM_VALUE) ||
                cleanType.startsWith("image/") ||
                cleanType.startsWith("audio/") ||
                cleanType.startsWith("video/")) {
            return false;
        }

        if (SUPPORTED_TYPES.contains(cleanType)) {
            return true;
        }

        // Support vendor-specific JSON/XML types like application/vnd.api+json or application/hal+json
        return cleanType.endsWith("+json") || cleanType.endsWith("+xml");
    }
}
