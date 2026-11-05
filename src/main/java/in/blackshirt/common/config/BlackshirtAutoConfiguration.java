package in.blackshirt.common.config;

import in.blackshirt.common.exception.handler.BusinessExceptionHandler;
import in.blackshirt.common.exception.handler.DatabaseExceptionHandler;
import in.blackshirt.common.exception.handler.FallbackExceptionHandler;
import in.blackshirt.common.exception.handler.HttpExceptionHandler;
import in.blackshirt.common.exception.handler.JavaRuntimeExceptionHandler;
import in.blackshirt.common.logging.JsonPayloadMasker;
import in.blackshirt.common.logging.MaskingCustomizer;
import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.logging.RequestResponseLoggingFilter;
import in.blackshirt.common.response.ResponseFactory;
import in.blackshirt.common.tenant.TenantClientRequestInterceptor;
import in.blackshirt.common.tenant.TenantContextFilter;
import in.blackshirt.common.tenant.TenantContextProvider;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(BlackshirtProperties.class)
public class BlackshirtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResponseFactory.class)
    public ResponseFactory responseFactory(ObjectProvider<Tracer> tracerProvider, BlackshirtProperties properties) {
        return new ResponseFactory(
                tracerProvider != null ? tracerProvider.getIfAvailable(() -> Tracer.NOOP) : Tracer.NOOP, properties);
    }

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

    // ──────────────── Exception Handlers ────────────────

    @Bean
    @ConditionalOnMissingBean(HttpExceptionHandler.class)
    @ConditionalOnProperty(prefix = "blackshirt.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpExceptionHandler httpExceptionHandler(ObjectProvider<Tracer> tracerProvider,
            HttpServletRequest request,
            BlackshirtProperties properties,
            ObjectProvider<PayloadMasker> payloadMaskerProvider) {
        return new HttpExceptionHandler(tracerProvider.getIfAvailable(() -> Tracer.NOOP), request, properties,
                payloadMaskerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(DatabaseExceptionHandler.class)
    @ConditionalOnClass(name = "org.springframework.dao.DataAccessException")
    @ConditionalOnProperty(prefix = "blackshirt.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DatabaseExceptionHandler databaseExceptionHandler(ObjectProvider<Tracer> tracerProvider,
            HttpServletRequest request,
            BlackshirtProperties properties,
            ObjectProvider<PayloadMasker> payloadMaskerProvider) {
        return new DatabaseExceptionHandler(tracerProvider.getIfAvailable(() -> Tracer.NOOP), request, properties,
                payloadMaskerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(BusinessExceptionHandler.class)
    @ConditionalOnProperty(prefix = "blackshirt.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BusinessExceptionHandler businessExceptionHandler(ObjectProvider<Tracer> tracerProvider,
            HttpServletRequest request,
            BlackshirtProperties properties,
            ObjectProvider<PayloadMasker> payloadMaskerProvider) {
        return new BusinessExceptionHandler(tracerProvider.getIfAvailable(() -> Tracer.NOOP), request, properties,
                payloadMaskerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(JavaRuntimeExceptionHandler.class)
    @ConditionalOnProperty(prefix = "blackshirt.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JavaRuntimeExceptionHandler javaRuntimeExceptionHandler(ObjectProvider<Tracer> tracerProvider,
            HttpServletRequest request,
            BlackshirtProperties properties,
            ObjectProvider<PayloadMasker> payloadMaskerProvider) {
        return new JavaRuntimeExceptionHandler(tracerProvider.getIfAvailable(() -> Tracer.NOOP), request, properties,
                payloadMaskerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(FallbackExceptionHandler.class)
    @ConditionalOnProperty(prefix = "blackshirt.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FallbackExceptionHandler fallbackExceptionHandler(ObjectProvider<Tracer> tracerProvider,
            HttpServletRequest request,
            BlackshirtProperties properties,
            ObjectProvider<PayloadMasker> payloadMaskerProvider) {
        return new FallbackExceptionHandler(tracerProvider.getIfAvailable(() -> Tracer.NOOP), request, properties,
                payloadMaskerProvider.getIfAvailable());
    }

    // ──────────────── Tenant Context ────────────────

    @Bean
    @ConditionalOnMissingBean(TenantContextProvider.class)
    @ConditionalOnProperty(prefix = "blackshirt.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TenantContextProvider tenantContextProvider(BlackshirtProperties properties) {
        return new TenantContextProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantContextFilter.class)
    @ConditionalOnProperty(prefix = "blackshirt.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TenantContextFilter tenantContextFilter(TenantContextProvider tenantContextProvider,
            BlackshirtProperties properties) {
        return new TenantContextFilter(tenantContextProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantClientRequestInterceptor.class)
    @ConditionalOnProperty(prefix = "blackshirt.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TenantClientRequestInterceptor tenantClientRequestInterceptor(TenantContextProvider tenantContextProvider) {
        return new TenantClientRequestInterceptor(tenantContextProvider);
    }
}
