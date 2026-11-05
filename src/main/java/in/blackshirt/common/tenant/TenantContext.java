package in.blackshirt.common.tenant;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Standard POJO representing tenant and invocation coordinates across microservices.
 * Contains {@code tenantId}, {@code serviceName}, {@code timestamp}, and optional {@code userId}.
 */
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final TenantContext EMPTY = new TenantContext(null, null, null, null);

    private String tenantId;
    private String serviceName;
    private Instant timestamp;
    private String userId;

    /**
     * Default constructor for frameworks and serialization.
     * Initializes timestamp to current instant.
     */
    public TenantContext() {
        this.timestamp = Instant.now();
    }

    public TenantContext(String tenantId, String serviceName) {
        this(tenantId, serviceName, Instant.now(), null);
    }

    public TenantContext(String tenantId, String serviceName, String userId) {
        this(tenantId, serviceName, Instant.now(), userId);
    }

    public TenantContext(String tenantId, String serviceName, Instant timestamp, String userId) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.userId = userId;
    }

    // ──────────────── Getters and Setters ────────────────

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Alias for {@link #getServiceName()} representing the calling service coordinate.
     */
    public String getCallerService() {
        return serviceName;
    }

    public void setCallerService(String callerService) {
        this.serviceName = callerService;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Optional<String> getOptionalUserId() {
        return Optional.ofNullable(userId);
    }

    /**
     * Returns true if a valid, non-blank tenant identifier is present.
     */
    public boolean isPresent() {
        return tenantId != null && !tenantId.isBlank();
    }

    /**
     * Returns a detached deep copy of this {@link TenantContext}.
     */
    public TenantContext copy() {
        return new TenantContext(tenantId, serviceName, timestamp, userId);
    }

    // ──────────────── Factory Methods ────────────────

    public static TenantContext of(String tenantId, String serviceName) {
        return new TenantContext(tenantId, serviceName);
    }

    public static TenantContext of(String tenantId, String serviceName, String userId) {
        return new TenantContext(tenantId, serviceName, userId);
    }

    public static TenantContext of(String tenantId, String serviceName, Instant timestamp, String userId) {
        return new TenantContext(tenantId, serviceName, timestamp, userId);
    }

    // ──────────────── Thread-Local Delegate Accessors ────────────────

    /**
     * Retrieves the current thread-bound {@link TenantContext} POJO from {@link TenantContextHolder}.
     */
    public static TenantContext get() {
        return TenantContextHolder.get();
    }

    /**
     * Binds the given {@link TenantContext} POJO to the current thread via {@link TenantContextHolder}.
     */
    public static void set(TenantContext context) {
        TenantContextHolder.set(context);
    }

    /**
     * Clears the current thread-bound {@link TenantContext}.
     */
    public static void clear() {
        TenantContextHolder.clear();
    }

    /**
     * Retrieves an {@link Optional} containing the thread-bound {@link TenantContext} if populated.
     */
    public static Optional<TenantContext> getCurrent() {
        return TenantContextHolder.getCurrent();
    }

    /**
     * Convenience shortcut to get the current tenant ID from thread context.
     */
    public static String currentTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * Convenience shortcut to get the current service name from thread context.
     */
    public static String currentServiceName() {
        return TenantContextHolder.getServiceName();
    }

    // ──────────────── Object Overrides ────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantContext that = (TenantContext) o;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, serviceName, timestamp, userId);
    }

    @Override
    public String toString() {
        return "TenantContext{" +
                "tenantId='" + tenantId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                '}';
    }
}
