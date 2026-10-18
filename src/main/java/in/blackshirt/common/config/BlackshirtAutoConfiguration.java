package in.blackshirt.common.config;

import in.blackshirt.common.exception.GlobalExceptionHandler;
import in.blackshirt.common.logging.JsonPayloadMasker;
import in.blackshirt.common.logging.MaskingCustomizer;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.logging.RequestResponseLoggingFilter;
import in.blackshirt.common.response.ResponseFactory;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(BlackshirtProperties.class)
@Import({
        GlobalExceptionHandler.class,
        ResponseFactory.class
})
public class BlackshirtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PayloadMasker.class)
    public PayloadMasker payloadMasker(BlackshirtProperties properties,
                                       ObjectProvider<List<MaskingCustomizer>> customizersProvider) {
        List<MaskingCustomizer> customizers = customizersProvider.getIfAvailable();
        return new JsonPayloadMasker(properties, customizers);
    }

    @Bean
    @ConditionalOnMissingBean(RequestResponseLoggingFilter.class)
    @ConditionalOnProperty(prefix = "blackshirt.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RequestResponseLoggingFilter requestResponseLoggingFilter(BlackshirtProperties properties,
                                                                     PayloadMasker payloadMasker,
                                                                     ObjectProvider<Tracer> tracerProvider) {
        return new RequestResponseLoggingFilter(properties, payloadMasker, tracerProvider);
    }
}
