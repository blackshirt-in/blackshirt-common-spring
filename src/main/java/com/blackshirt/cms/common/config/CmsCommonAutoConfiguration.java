package com.blackshirt.cms.common.config;

import com.blackshirt.cms.common.exception.GlobalExceptionHandler;
import com.blackshirt.cms.common.logging.LoggingAspect;
import com.blackshirt.cms.common.filter.MdcFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfiguration
@Import({
    LoggingProperties.class,
    LoggingAspect.class,
    GlobalExceptionHandler.class
})
public class CmsCommonAutoConfiguration {

    @Bean
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(mdcFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
