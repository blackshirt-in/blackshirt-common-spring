package in.blackshirt.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantContext POJO has standard getters, setters, and no-arg constructor")
    void testTenantContextPojo() {
        TenantContext context = new TenantContext();
        assertNull(context.getTenantId());
        assertNull(context.getServiceName());
        assertNull(context.getUserId());
        assertNotNull(context.getTimestamp());
        assertFalse(context.isPresent());

        context.setTenantId("tenant-123");
        context.setServiceName("order-service");
        context.setCallerService("checkout-service");
        context.setUserId("user-88");
        Instant customTime = Instant.now();
        context.setTimestamp(customTime);

        assertEquals("tenant-123", context.getTenantId());
        assertEquals("checkout-service", context.getServiceName());
        assertEquals("checkout-service", context.getCallerService());
        assertEquals("user-88", context.getUserId());
        assertEquals(customTime, context.getTimestamp());
        assertTrue(context.isPresent());
        assertTrue(context.getOptionalUserId().isPresent());
        assertEquals("user-88", context.getOptionalUserId().get());

        TenantContext copy = context.copy();
        assertEquals(context, copy);
        assertEquals(context.hashCode(), copy.hashCode());
        assertTrue(context.toString().contains("tenant-123"));
    }

    @Test
    @DisplayName("TenantContext returns EMPTY when not set")
    void testEmptyContext() {
        TenantContext context = TenantContext.get();
        assertNotNull(context);
        assertFalse(context.isPresent());
        assertFalse(TenantContext.getCurrent().isPresent());
    }

    @Test
    @DisplayName("TenantContext correctly stores and retrieves TenantContext POJO")
    void testSetAndGet() {
        Instant now = Instant.now();
        TenantContext context = new TenantContext("tenant-100", "order-service", now, "user-42");

        TenantContext.set(context);

        TenantContext retrieved = TenantContext.get();
        assertEquals("tenant-100", retrieved.getTenantId());
        assertEquals("order-service", retrieved.getServiceName());
        assertEquals(now, retrieved.getTimestamp());
        assertEquals("user-42", retrieved.getUserId());
        assertTrue(retrieved.isPresent());

        assertEquals("tenant-100", TenantContext.currentTenantId());
        assertEquals("order-service", TenantContext.currentServiceName());
    }

    @Test
    @DisplayName("TenantContext clears properly")
    void testClear() {
        TenantContext.set(TenantContext.of("tenant-100", "order-service"));
        assertTrue(TenantContext.get().isPresent());

        TenantContext.clear();

        assertFalse(TenantContext.get().isPresent());
    }

    @Test
    @DisplayName("InheritableThreadLocal propagates tenant context to child threads")
    void testThreadInheritance() throws InterruptedException {
        TenantContext.set(TenantContext.of("tenant-abc", "cart-service", "user-99"));

        AtomicReference<String> childTenantId = new AtomicReference<>();
        AtomicReference<String> childUserId = new AtomicReference<>();

        Thread childThread = new Thread(() -> {
            childTenantId.set(TenantContext.get().getTenantId());
            childUserId.set(TenantContext.get().getUserId());
        });

        childThread.start();
        childThread.join();

        assertEquals("tenant-abc", childTenantId.get());
        assertEquals("user-99", childUserId.get());
    }

    @Test
    @DisplayName("TenantContext static factory methods construct expected POJO")
    void testTenantContextFactories() {
        TenantContext contextWithoutUser = TenantContext.of("t-1", "s-1");
        assertEquals("t-1", contextWithoutUser.getTenantId());
        assertEquals("s-1", contextWithoutUser.getServiceName());
        assertNotNull(contextWithoutUser.getTimestamp());
        assertNull(contextWithoutUser.getUserId());

        TenantContext contextWithUser = TenantContext.of("t-2", "s-2", "u-2");
        assertEquals("t-2", contextWithUser.getTenantId());
        assertEquals("u-2", contextWithUser.getUserId());
    }
}
