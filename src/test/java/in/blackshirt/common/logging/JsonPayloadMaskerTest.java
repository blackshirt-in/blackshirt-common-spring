package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPayloadMaskerTest {

    @Test
    @DisplayName("Default masking sanitizes standard password, token, and secret fields")
    void testDefaultJsonMasking() {
        BlackshirtProperties properties = new BlackshirtProperties();
        JsonPayloadMasker masker = new JsonPayloadMasker(properties, null);

        String json = "{\"username\": \"john\", \"password\": \"mySecretP@ss\", \"token\": \"jwt-abc-123\", \"pin\": 9876}";
        String masked = masker.maskPayload(json);

        assertTrue(masked.contains("\"username\": \"john\""));
        assertTrue(masked.contains("\"password\": \"***\""));
        assertTrue(masked.contains("\"token\": \"***\""));
        assertTrue(masked.contains("\"pin\": \"***\""));
        assertFalse(masked.contains("mySecretP@ss"));
        assertFalse(masked.contains("jwt-abc-123"));
        assertFalse(masked.contains("9876"));
    }

    @Test
    @DisplayName("Property-driven additionalMaskedFields sanitizes client domain fields")
    void testAdditionalMaskedFields() {
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLogging().getMasking().getAdditionalMaskedFields().add("recipientPhone");
        properties.getLogging().getMasking().getAdditionalMaskedFields().add("otpCode");

        JsonPayloadMasker masker = new JsonPayloadMasker(properties, null);

        String json = "{\"recipientPhone\": \"+919876543210\", \"otpCode\": 4567, \"template\": \"OTP_SMS\"}";
        String masked = masker.maskPayload(json);

        assertTrue(masked.contains("\"template\": \"OTP_SMS\""));
        assertTrue(masked.contains("\"recipientPhone\": \"***\""));
        assertTrue(masked.contains("\"otpCode\": \"***\""));
        assertFalse(masked.contains("+919876543210"));
        assertFalse(masked.contains("4567"));
    }

    @Test
    @DisplayName("Custom MaskingCustomizer hook allows programmatic client-code registration")
    void testMaskingCustomizer() {
        BlackshirtProperties properties = new BlackshirtProperties();
        MaskingCustomizer customizer = config -> {
            config.setMaskReplacement("[PROTECTED]");
            config.getAdditionalMaskedFields().add("webhookSecret");
        };

        JsonPayloadMasker masker = new JsonPayloadMasker(properties, List.of(customizer));

        String json = "{\"webhookSecret\": \"whsec_abcdef123\", \"service\": \"orders\"}";
        String masked = masker.maskPayload(json);

        assertTrue(masked.contains("\"webhookSecret\": \"[PROTECTED]\""));
        assertTrue(masked.contains("\"service\": \"orders\""));
        assertFalse(masked.contains("whsec_abcdef123"));
    }

    @Test
    @DisplayName("Masking sanitizes sensitive HTTP headers")
    void testHeaderMasking() {
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLogging().getMasking().getAdditionalMaskedHeaders().add("X-Custom-Auth");

        JsonPayloadMasker masker = new JsonPayloadMasker(properties, null);

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer eyJhbGciOi...",
                "Cookie", "SESSIONID=xyz",
                "X-Custom-Auth", "custom-secret-key",
                "Content-Type", "application/json"
        );

        Map<String, String> masked = masker.maskHeaders(headers);

        assertEquals("***", masked.get("Authorization"));
        assertEquals("***", masked.get("Cookie"));
        assertEquals("***", masked.get("X-Custom-Auth"));
        assertEquals("application/json", masked.get("Content-Type"));
    }

    @Test
    @DisplayName("Form-urlencoded payloads are safely masked")
    void testFormUrlEncodedMasking() {
        BlackshirtProperties properties = new BlackshirtProperties();
        JsonPayloadMasker masker = new JsonPayloadMasker(properties, null);

        String form = "username=admin&password=supersecret&token=abc12345&grant_type=password";
        String masked = masker.maskPayload(form);

        assertTrue(masked.contains("username=admin"));
        assertTrue(masked.contains("password=***"));
        assertTrue(masked.contains("token=***"));
        assertTrue(masked.contains("grant_type=password"));
        assertFalse(masked.contains("supersecret"));
        assertFalse(masked.contains("abc12345"));
    }

    @Test
    @DisplayName("Masking can be disabled via property blackshirt.logging.masking.enabled=false")
    void testDisabledMasking() {
        BlackshirtProperties properties = new BlackshirtProperties();
        properties.getLogging().getMasking().setEnabled(false);

        JsonPayloadMasker masker = new JsonPayloadMasker(properties, null);

        String json = "{\"password\": \"secret\"}";
        assertEquals(json, masker.maskPayload(json));
    }
}
