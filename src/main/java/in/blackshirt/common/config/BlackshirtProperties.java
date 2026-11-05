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
    private ExceptionHandlerProperties exceptionHandler = new ExceptionHandlerProperties();
    private TenantProperties tenant = new TenantProperties();

    public static class Log {
        private String serviceName = "unknown-service";

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName != null && !serviceName.isBlank() ? serviceName : "unknown-service";
        }
    }

    public String getServiceName() {
        return log != null ? log.getServiceName() : "unknown-service";
    }

    public static class Logging {
        private boolean enabled = true;
        private boolean includeHeaders = false;
        private boolean includePayload = false;
        private boolean includeQueryParams = true;
        private boolean includePathVariables = true;
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
                    "refreshToken", "authorization", "cvv", "pin", "cardNumber",
                    "apiKey", "access_token", "refresh_token", "creditCard", "ssn"
            ));
            private Set<String> additionalMaskedFields = new HashSet<>();

            private Set<String> maskedHeaders = new HashSet<>(Set.of(
                    "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token"
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

        public boolean isIncludeQueryParams() {
            return includeQueryParams;
        }

        public void setIncludeQueryParams(boolean includeQueryParams) {
            this.includeQueryParams = includeQueryParams;
        }

        public boolean isIncludePathVariables() {
            return includePathVariables;
        }

        public void setIncludePathVariables(boolean includePathVariables) {
            this.includePathVariables = includePathVariables;
        }
    }

    public static class TenantProperties {
        private boolean enabled = true;
        private String tenantHeaderName = "X-Tenant-Id";
        private String serviceHeaderName = "X-Service-Name";
        private String userHeaderName = "X-User-Id";
        private String timestampHeaderName = "X-Timestamp";
        private boolean requireTenantId = false;
        private boolean propagateToMdc = true;
        private String tenantMdcKey = "tenantId";
        private String serviceMdcKey = "callerService";
        private String userMdcKey = "userId";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTenantHeaderName() {
            return tenantHeaderName;
        }

        public void setTenantHeaderName(String tenantHeaderName) {
            this.tenantHeaderName = tenantHeaderName;
        }

        public String getServiceHeaderName() {
            return serviceHeaderName;
        }

        public void setServiceHeaderName(String serviceHeaderName) {
            this.serviceHeaderName = serviceHeaderName;
        }

        public String getUserHeaderName() {
            return userHeaderName;
        }

        public void setUserHeaderName(String userHeaderName) {
            this.userHeaderName = userHeaderName;
        }

        public String getTimestampHeaderName() {
            return timestampHeaderName;
        }

        public void setTimestampHeaderName(String timestampHeaderName) {
            this.timestampHeaderName = timestampHeaderName;
        }

        public boolean isRequireTenantId() {
            return requireTenantId;
        }

        public void setRequireTenantId(boolean requireTenantId) {
            this.requireTenantId = requireTenantId;
        }

        public boolean isPropagateToMdc() {
            return propagateToMdc;
        }

        public void setPropagateToMdc(boolean propagateToMdc) {
            this.propagateToMdc = propagateToMdc;
        }

        public String getTenantMdcKey() {
            return tenantMdcKey;
        }

        public void setTenantMdcKey(String tenantMdcKey) {
            this.tenantMdcKey = tenantMdcKey;
        }

        public String getServiceMdcKey() {
            return serviceMdcKey;
        }

        public void setServiceMdcKey(String serviceMdcKey) {
            this.serviceMdcKey = serviceMdcKey;
        }

        public String getUserMdcKey() {
            return userMdcKey;
        }

        public void setUserMdcKey(String userMdcKey) {
            this.userMdcKey = userMdcKey;
        }
    }

    public static class ExceptionHandlerProperties {
        private boolean enabled = true;
        private boolean includeStackTrace = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeStackTrace() {
            return includeStackTrace;
        }

        public void setIncludeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
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

    public ExceptionHandlerProperties getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandlerProperties exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public TenantProperties getTenant() {
        return tenant;
    }

    public void setTenant(TenantProperties tenant) {
        this.tenant = tenant;
    }
}
