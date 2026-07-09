package in.blackshirt.common.config;

import jakarta.annotation.Nonnull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "blackshirt")
public record BlackshirtProperties(
        Log log) {
    public record Log(
            @Nonnull String serviceName) {
    }
}
