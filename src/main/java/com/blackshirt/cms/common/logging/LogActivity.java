package com.blackshirt.cms.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for centralized logging using AOP.
 * Can be used to capture execution time, arguments, and return values.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    
    /**
     * Description or name of the action being logged.
     */
    String value() default "";

    /**
     * Whether to log the method arguments. Defaults to true.
     */
    boolean logArgs() default true;

    /**
     * Whether to log the method return value. Defaults to true.
     */
    boolean logResult() default true;
}
