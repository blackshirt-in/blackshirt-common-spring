package com.blackshirt.cms.common.logging;

import com.blackshirt.cms.common.config.LoggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@ConditionalOnProperty(prefix = "cms.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {

    private final LoggingProperties loggingProperties;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    public LoggingAspect(LoggingProperties loggingProperties, 
                         @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.loggingProperties = loggingProperties;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.blackshirt.cms.common.logging.LogActivity)")
    public Object logActivityAspect(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogActivity logActivity = method.getAnnotation(LogActivity.class);

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String actionName = logActivity.value().isEmpty() ? className + "." + methodName : logActivity.value();

        // Trace ID management via MDC
        boolean isNewTraceId = false;
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
            isNewTraceId = true;
        }

        long start = System.currentTimeMillis();

        try {
            if (logActivity.logArgs() && logger.isInfoEnabled()) {
                String args = (objectMapper != null) ? objectMapper.writeValueAsString(joinPoint.getArgs()) : "Args logging enabled but ObjectMapper missing";
                logger.info("[{}] STARTED - Args: {}", actionName, args);
            } else {
                logger.info("[{}] STARTED", actionName);
            }

            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - start;

            if (logActivity.logResult() && logger.isInfoEnabled()) {
                String stringResult = null;
                if (objectMapper != null) {
                    stringResult = objectMapper.writeValueAsString(result);
                    // Truncate response if it's too large to prevent blowing up logs
                    if (stringResult != null && stringResult.length() > 2000) {
                        stringResult = stringResult.substring(0, 2000) + "... [TRUNCATED]";
                    }
                } else {
                    stringResult = "Result logging enabled but ObjectMapper missing";
                }
                logger.info("[{}] COMPLETED - Result: {}", actionName, stringResult);
            } else {
                logger.info("[{}] COMPLETED", actionName);
            }

            if (loggingProperties.isLogExecutionTime()) {
                logger.info("[{}] EXECUTED IN {} ms", actionName, executionTime);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            logger.error("[{}] FAILED - Error: {}", actionName, e.getMessage());
            if (loggingProperties.isLogExecutionTime()) {
                logger.info("[{}] EXECUTED IN {} ms BEFORE FAILURE", actionName, executionTime);
            }
            throw e;
        } finally {
            if (isNewTraceId) {
                MDC.remove("traceId");
            }
        }
    }
}
