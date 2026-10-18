package in.blackshirt.common.logging;

import in.blackshirt.common.config.BlackshirtProperties;

/**
 * Functional interface that allows client microservices to programmatically
 * customize masking configuration (e.g. adding custom fields or headers)
 * by defining a {@code @Bean} of this type.
 */
@FunctionalInterface
public interface MaskingCustomizer {

    /**
     * Customize the masking configuration during application startup.
     *
     * @param maskingConfig the mutable masking configuration from properties
     */
    void customize(BlackshirtProperties.Logging.Masking maskingConfig);
}
