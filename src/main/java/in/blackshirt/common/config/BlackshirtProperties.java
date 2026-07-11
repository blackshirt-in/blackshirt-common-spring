package in.blackshirt.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blackshirt")
public class BlackshirtProperties {

        private Log log = new Log();

        public static class Log {
                private String serviceName;

                public void setServiceName(String serviceName) {
                        this.serviceName = serviceName;
                }

                public String getServiceName() {
                        return serviceName;
                }
        }

        public void setLog(Log log) {
                this.log = log;
        }

        public Log getLog() {
                return this.log;
        }
}
