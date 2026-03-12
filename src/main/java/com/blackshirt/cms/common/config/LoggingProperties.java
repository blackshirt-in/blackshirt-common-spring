package com.blackshirt.cms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cms.logging")
public class LoggingProperties {
    
    /**
     * Enable or disable centralized logging aspect.
     * Default is true.
     */
    private boolean enabled = true;

    /**
     * Whether to log performance metrics (execution time).
     */
    private boolean logExecutionTime = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogExecutionTime() {
        return logExecutionTime;
    }

    public void setLogExecutionTime(boolean logExecutionTime) {
        this.logExecutionTime = logExecutionTime;
    }
}
