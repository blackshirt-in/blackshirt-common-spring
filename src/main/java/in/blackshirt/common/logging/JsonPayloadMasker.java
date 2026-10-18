package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link PayloadMasker} that sanitizes sensitive
 * JSON fields, form-urlencoded key-value pairs, and HTTP headers based on properties
 * and any registered {@link MaskingCustomizer} beans.
 */
public class JsonPayloadMasker implements PayloadMasker {

    private final BlackshirtProperties.Logging.Masking config;
    private final Set<String> effectiveHeaders;
    private final List<Pattern> jsonStringPatterns;
    private final List<Pattern> jsonPrimitivePatterns;
    private final List<Pattern> formUrlEncodedPatterns;

    public JsonPayloadMasker(BlackshirtProperties properties, List<MaskingCustomizer> customizers) {
        this.config = properties.getLogging().getMasking();
        if (customizers != null) {
            for (MaskingCustomizer customizer : customizers) {
                if (customizer != null) {
                    customizer.customize(this.config);
                }
            }
        }

        Set<String> effectiveFields = this.config.getEffectiveMaskedFields();
        this.effectiveHeaders = this.config.getEffectiveMaskedHeaders();

        List<Pattern> stringPatterns = new ArrayList<>();
        List<Pattern> primitivePatterns = new ArrayList<>();
        List<Pattern> formPatterns = new ArrayList<>();

        for (String field : effectiveFields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            String quoted = Pattern.quote(field.trim());
            // Matches "field": "value" (handling escaped quotes within JSON string values)
            stringPatterns.add(Pattern.compile("(?i)(\"" + quoted + "\"\\s*:\\s*\")([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)(\")"));
            // Matches "field": 1234 or "field": true / false
            primitivePatterns.add(Pattern.compile("(?i)(\"" + quoted + "\"\\s*:\\s*)([0-9]+(?:\\.[0-9]+)?|true|false)"));
            // Matches field=value in form-urlencoded or query strings
            formPatterns.add(Pattern.compile("(?i)(^|[&?])(" + quoted + "=)([^&]*)"));
        }

        this.jsonStringPatterns = Collections.unmodifiableList(stringPatterns);
        this.jsonPrimitivePatterns = Collections.unmodifiableList(primitivePatterns);
        this.formUrlEncodedPatterns = Collections.unmodifiableList(formPatterns);
    }

    @Override
    public String maskPayload(String payload) {
        if (!config.isEnabled() || payload == null || payload.isBlank()) {
            return payload;
        }

        String replacement = config.getMaskReplacement();
        String result = payload;

        String quotedReplacement = java.util.regex.Matcher.quoteReplacement(replacement);

        // Apply JSON string masking: "field": "secret" -> "field": "***"
        for (Pattern pattern : jsonStringPatterns) {
            result = pattern.matcher(result).replaceAll("$1" + quotedReplacement + "$3");
        }

        // Apply JSON primitive masking: "field": 1234 -> "field": "***"
        for (Pattern pattern : jsonPrimitivePatterns) {
            result = pattern.matcher(result).replaceAll("$1\"" + quotedReplacement + "\"");
        }

        // Apply Form-urlencoded masking: field=secret -> field=***
        for (Pattern pattern : formUrlEncodedPatterns) {
            result = pattern.matcher(result).replaceAll("$1$2" + quotedReplacement);
        }

        return result;
    }

    @Override
    public Map<String, String> maskHeaders(Map<String, String> headers) {
        if (!config.isEnabled() || headers == null || headers.isEmpty()) {
            return headers != null ? headers : Collections.emptyMap();
        }

        Map<String, String> masked = new LinkedHashMap<>();
        String replacement = config.getMaskReplacement();

        headers.forEach((name, value) -> {
            if (name != null && effectiveHeaders.contains(name)) {
                masked.put(name, replacement);
            } else {
                masked.put(name, value);
            }
        });

        return masked;
    }
}
