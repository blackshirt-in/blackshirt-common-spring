package in.blackshirt.common.config;

import in.blackshirt.common.logging.PayloadMasker;
import in.blackshirt.common.logging.RequestResponseLoggingFilter;
import in.blackshirt.common.response.ResponseFactory;
import in.blackshirt.common.tenant.TenantClientRequestInterceptor;
import in.blackshirt.common.tenant.TenantContextFilter;
import in.blackshirt.common.tenant.TenantContextProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BlackshirtAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BlackshirtAutoConfiguration.class));

    @Test
    @DisplayName("Auto-configuration loads core beans by default")
    void testDefaultBeansLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BlackshirtProperties.class);
            assertThat(context).hasSingleBean(PayloadMasker.class);
            assertThat(context).hasSingleBean(RequestResponseLoggingFilter.class);
            assertThat(context).hasSingleBean(ResponseFactory.class);
            assertThat(context).hasSingleBean(TenantContextProvider.class);
            assertThat(context).hasSingleBean(TenantContextFilter.class);
            assertThat(context).hasSingleBean(TenantClientRequestInterceptor.class);
        });
    }

    @Test
    @DisplayName("Auto-configuration honors disabled properties")
    void testDisabledProperties() {
        contextRunner
                .withPropertyValues(
                        "blackshirt.logging.enabled=false",
                        "blackshirt.tenant.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RequestResponseLoggingFilter.class);
                    assertThat(context).doesNotHaveBean(TenantContextProvider.class);
                    assertThat(context).doesNotHaveBean(TenantContextFilter.class);
                    assertThat(context).doesNotHaveBean(TenantClientRequestInterceptor.class);
                    assertThat(context).hasSingleBean(ResponseFactory.class);
                });
    }
}

