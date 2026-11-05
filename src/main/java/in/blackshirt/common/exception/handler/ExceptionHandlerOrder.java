package in.blackshirt.common.exception.handler;

import org.springframework.core.Ordered;

/**
 * Defines priority ordering constants for Blackshirt common exception handlers.
 *
 * <p>Handlers run in ascending order of their numeric value (lowest value = highest precedence).
 * They are positioned near {@link Ordered#LOWEST_PRECEDENCE} so that client microservices
 * can define custom {@code @RestControllerAdvice} beans with higher precedence (e.g.
 * {@code @Order(Ordered.HIGHEST_PRECEDENCE)} or {@code @Order(1)}) to intercept domain-specific
 * exceptions before the common library.
 */
public final class ExceptionHandlerOrder {

    private ExceptionHandlerOrder() {}

    /**
     * 1st in common pipeline: Web / HTTP / MVC framework errors (400, 404, 405, 415, validation).
     */
    public static final int HTTP_HANDLER = Ordered.LOWEST_PRECEDENCE - 40;

    /**
     * 2nd in common pipeline: SQL, JDBC, and Spring DAO persistence errors.
     */
    public static final int DATABASE_HANDLER = Ordered.LOWEST_PRECEDENCE - 30;

    /**
     * 3rd in common pipeline: Application and business domain exceptions (BaseException).
     */
    public static final int BUSINESS_HANDLER = Ordered.LOWEST_PRECEDENCE - 20;

    /**
     * 4th in common pipeline: Standard Java runtime exceptions (NPE, IllegalArgument, etc.).
     */
    public static final int RUNTIME_HANDLER = Ordered.LOWEST_PRECEDENCE - 10;

    /**
     * 5th / Fallback: Generic catch-all for any unhandled Throwable / Exception (500).
     */
    public static final int FALLBACK_HANDLER = Ordered.LOWEST_PRECEDENCE;
}

