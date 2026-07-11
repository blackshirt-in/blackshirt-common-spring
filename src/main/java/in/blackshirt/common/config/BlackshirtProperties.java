package in.blackshirt.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blackshirt")
public class BlackshirtProperties {

        private Log log;

        class Log {
                private String serviceName;

                public void setServiceName(String serviceName) {
                        this.serviceName = serviceName;
                }
        }
}
