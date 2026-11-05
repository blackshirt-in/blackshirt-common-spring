package in.blackshirt.common.tenant;

import java.util.Optional;

/**
 * Thread-local holder for the current thread's {@link TenantContext} POJO.
 * Backed by {@link InheritableThreadLocal} so child threads, async executors,
 * and parallel streams automatically inherit the tenant context.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CURRENT = new InheritableThreadLocal<>();

    private TenantContextHolder() {}

    /**
     * Binds the given {@link TenantContext} to the current thread.
     * Passing {@code null} will clear the context.
     */
    public static void set(TenantContext context) {
        if (context == null) {
            clear();
        } else {
            CURRENT.set(context);
        }
    }

    /**
     * Retrieves the current thread's {@link TenantContext}.
     * Never returns {@code null}; returns {@link TenantContext#EMPTY} if no context is bound.
     */
    public static TenantContext get() {
        TenantContext context = CURRENT.get();
        return context != null ? context : TenantContext.EMPTY;
    }

    /**
     * Retrieves an {@link Optional} containing the {@link TenantContext} if present and populated.
     */
    public static Optional<TenantContext> getCurrent() {
        TenantContext context = CURRENT.get();
        return (context != null && context.isPresent()) ? Optional.of(context) : Optional.empty();
    }

    /**
     * Convenience shortcut to get the current tenant ID.
     */
    public static String getTenantId() {
        return get().getTenantId();
    }

    /**
     * Convenience shortcut to get the service name.
     */
    public static String getServiceName() {
        return get().getServiceName();
    }

    /**
     * Convenience shortcut to get the caller service name.
     */
    public static String getCallerService() {
        return get().getCallerService();
    }

    /**
     * Convenience shortcut to get the user ID.
     */
    public static String getUserId() {
        return get().getUserId();
    }

    /**
     * Convenience shortcut to get the optional user ID.
     */
    public static Optional<String> getOptionalUserId() {
        return get().getOptionalUserId();
    }

    /**
     * Clears the tenant context for the current thread.
     */
    public static void clear() {
        CURRENT.remove();
    }
}

