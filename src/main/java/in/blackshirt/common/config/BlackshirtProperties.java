package in.blackshirt.common.config;

import jakarta.annotation.Nonnull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blackshirt")
public record BlackshirtProperties(
        Log log) {
    public record Log(
            @Nonnull String serviceName) {
    }
}
