package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance implementation of {@link PayloadMasker} that sanitizes sensitive
 * JSON fields, form-urlencoded key-value pairs, and HTTP headers using pre-compiled
 * consolidated regular expressions and single-pass replacements.
 */
public class JsonPayloadMasker implements PayloadMasker {

    private final BlackshirtProperties.Logging.Masking config;
    private final Set<String> effectiveHeaders;
    private final Pattern jsonStringPattern;
    private final Pattern jsonPrimitivePattern;
    private final Pattern formUrlEncodedPattern;

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

        StringBuilder fieldAlternation = new StringBuilder();
        for (String field : effectiveFields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            if (fieldAlternation.length() > 0) {
                fieldAlternation.append('|');
            }
            fieldAlternation.append(Pattern.quote(field.trim()));
        }

        if (fieldAlternation.length() > 0) {
            String fields = fieldAlternation.toString();
            // Matches "field": "value" (handling escaped quotes within JSON string values)
            this.jsonStringPattern = Pattern.compile("(?i)(\"(?:" + fields + ")\"\\s*:\\s*\")([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)(\")");
            // Matches "field": 1234 or "field": true / false
            this.jsonPrimitivePattern = Pattern.compile("(?i)(\"(?:" + fields + ")\"\\s*:\\s*)([0-9]+(?:\\.[0-9]+)?|true|false)");
            // Matches field=value in form-urlencoded or query strings
            this.formUrlEncodedPattern = Pattern.compile("(?i)(^|[&?])((?:" + fields + ")=)([^&]*)");
        } else {
            this.jsonStringPattern = null;
            this.jsonPrimitivePattern = null;
            this.formUrlEncodedPattern = null;
        }
    }

    @Override
    public String maskPayload(String payload) {
        if (!config.isEnabled() || payload == null || payload.isBlank() || jsonStringPattern == null) {
            return payload;
        }

        String replacement = Matcher.quoteReplacement(config.getMaskReplacement());
        String result = payload;

        // Apply consolidated JSON string masking in a single regex pass
        result = jsonStringPattern.matcher(result).replaceAll("$1" + replacement + "$3");

        // Apply consolidated JSON primitive masking in a single regex pass
        result = jsonPrimitivePattern.matcher(result).replaceAll("$1\"" + replacement + "\"");

        // Apply consolidated form-urlencoded masking in a single regex pass
        result = formUrlEncodedPattern.matcher(result).replaceAll("$1$2" + replacement);

        return result;
    }

    @Override
    public String maskHeaderValue(String name, String value) {
        if (!config.isEnabled() || name == null) {
            return value;
        }
        return effectiveHeaders.contains(name) ? config.getMaskReplacement() : value;
    }

    @Override
    public Map<String, String> maskHeaders(Map<String, String> headers) {
        if (!config.isEnabled() || headers == null || headers.isEmpty()) {
            return headers != null ? headers : Collections.emptyMap();
        }

        Map<String, String> masked = new LinkedHashMap<>(headers.size());
        headers.forEach((name, value) -> masked.put(name, maskHeaderValue(name, value)));
        return masked;
    }
}
