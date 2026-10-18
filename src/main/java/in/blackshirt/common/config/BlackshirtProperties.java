package in.blackshirt.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ConfigurationProperties(prefix = "blackshirt")
public class BlackshirtProperties {

    private Log log = new Log();
    private Logging logging = new Logging();

    public static class Log {
        private String serviceName;

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    public static class Logging {
        private boolean enabled = true;
        private String traceHeaderName = "X-Trace-Id";
        private boolean includeHeaders = false;
        private boolean includePayload = false;
        private int maxPayloadLength = 4096;
        private List<String> excludePaths = new ArrayList<>(List.of(
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/error"
        ));
        private Masking masking = new Masking();

        public static class Masking {
            private boolean enabled = true;
            private String maskReplacement = "***";

            private Set<String> maskedFields = new HashSet<>(Set.of(
                    "password", "secret", "token", "accessToken",
                    "refreshToken", "authorization", "cvv", "pin", "cardNumber"
            ));
            private Set<String> additionalMaskedFields = new HashSet<>();

            private Set<String> maskedHeaders = new HashSet<>(Set.of(
                    "authorization", "cookie", "set-cookie", "x-api-key"
            ));
            private Set<String> additionalMaskedHeaders = new HashSet<>();

            public Set<String> getEffectiveMaskedFields() {
                Set<String> effective = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                if (maskedFields != null) {
                    effective.addAll(maskedFields);
                }
                if (additionalMaskedFields != null) {
                    effective.addAll(additionalMaskedFields);
                }
                return Collections.unmodifiableSet(effective);
            }

            public Set<String> getEffectiveMaskedHeaders() {
                Set<String> effective = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                if (maskedHeaders != null) {
                    effective.addAll(maskedHeaders);
                }
                if (additionalMaskedHeaders != null) {
                    effective.addAll(additionalMaskedHeaders);
                }
                return Collections.unmodifiableSet(effective);
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getMaskReplacement() {
                return maskReplacement;
            }

            public void setMaskReplacement(String maskReplacement) {
                this.maskReplacement = maskReplacement;
            }

            public Set<String> getMaskedFields() {
                return maskedFields;
            }

            public void setMaskedFields(Set<String> maskedFields) {
                this.maskedFields = maskedFields;
            }

            public Set<String> getAdditionalMaskedFields() {
                return additionalMaskedFields;
            }

            public void setAdditionalMaskedFields(Set<String> additionalMaskedFields) {
                this.additionalMaskedFields = additionalMaskedFields;
            }

            public Set<String> getMaskedHeaders() {
                return maskedHeaders;
            }

            public void setMaskedHeaders(Set<String> maskedHeaders) {
                this.maskedHeaders = maskedHeaders;
            }

            public Set<String> getAdditionalMaskedHeaders() {
                return additionalMaskedHeaders;
            }

            public void setAdditionalMaskedHeaders(Set<String> additionalMaskedHeaders) {
                this.additionalMaskedHeaders = additionalMaskedHeaders;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTraceHeaderName() {
            return traceHeaderName;
        }

        public void setTraceHeaderName(String traceHeaderName) {
            this.traceHeaderName = traceHeaderName;
        }

        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        public boolean isIncludePayload() {
            return includePayload;
        }

        public void setIncludePayload(boolean includePayload) {
            this.includePayload = includePayload;
        }

        public int getMaxPayloadLength() {
            return maxPayloadLength;
        }

        public void setMaxPayloadLength(int maxPayloadLength) {
            this.maxPayloadLength = maxPayloadLength;
        }

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }

        public Masking getMasking() {
            return masking;
        }

        public void setMasking(Masking masking) {
            this.masking = masking;
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Log getLog() {
        return this.log;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }
}
