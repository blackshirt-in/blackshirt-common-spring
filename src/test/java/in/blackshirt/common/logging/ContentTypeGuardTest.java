package in.blackshirt.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTypeGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "application/json",
            "application/json;charset=UTF-8",
            "application/problem+json",
            "application/xml",
            "text/plain",
            "application/x-www-form-urlencoded",
            "application/vnd.api+json",
            "application/hal+json"
    })
    @DisplayName("isLoggable returns true for supported text/JSON/XML content types")
    void testLoggableContentTypes(String contentType) {
        assertTrue(ContentTypeGuard.isLoggable(contentType));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "multipart/form-data",
            "multipart/form-data; boundary=----WebKitFormBoundary",
            "application/octet-stream",
            "text/event-stream",
            "image/png",
            "image/jpeg",
            "audio/mpeg",
            "video/mp4"
    })
    @DisplayName("isLoggable returns false for binary, multipart, or streaming content types")
    void testNonLoggableContentTypes(String contentType) {
        assertFalse(ContentTypeGuard.isLoggable(contentType));
    }

    @Test
    @DisplayName("isLoggable returns false for null or blank content types")
    void testNullOrBlank() {
        assertFalse(ContentTypeGuard.isLoggable(null));
        assertFalse(ContentTypeGuard.isLoggable(""));
        assertFalse(ContentTypeGuard.isLoggable("   "));
    }
}
