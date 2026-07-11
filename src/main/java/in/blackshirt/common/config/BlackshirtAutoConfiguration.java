package in.blackshirt.common.config;

import in.blackshirt.common.exception.GlobalExceptionHandler;
import in.blackshirt.common.response.ResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(BlackshirtProperties.class)
@Import({
        GlobalExceptionHandler.class,
        ResponseFactory.class
})
public class BlackshirtAutoConfiguration {
}
