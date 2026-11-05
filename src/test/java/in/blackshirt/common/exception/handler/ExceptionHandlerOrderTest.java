package in.blackshirt.common.exception.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionHandlerOrderTest {

    private int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        assertNotNull(order, "Handler " + clazz.getSimpleName() + " must have @Order annotation");
        return order.value();
    }

    @Test
    @DisplayName("Verify exception handlers have the correct relative order")
    void testHandlerOrdering() {
        int httpOrder = getOrder(HttpExceptionHandler.class);
        int dbOrder = getOrder(DatabaseExceptionHandler.class);
        int businessOrder = getOrder(BusinessExceptionHandler.class);
        int runtimeOrder = getOrder(JavaRuntimeExceptionHandler.class);
        int fallbackOrder = getOrder(FallbackExceptionHandler.class);

        assertEquals(ExceptionHandlerOrder.HTTP_HANDLER, httpOrder);
        assertEquals(ExceptionHandlerOrder.DATABASE_HANDLER, dbOrder);
        assertEquals(ExceptionHandlerOrder.BUSINESS_HANDLER, businessOrder);
        assertEquals(ExceptionHandlerOrder.RUNTIME_HANDLER, runtimeOrder);
        assertEquals(ExceptionHandlerOrder.FALLBACK_HANDLER, fallbackOrder);

        assertTrue(httpOrder < dbOrder, "HttpExceptionHandler should run before DatabaseExceptionHandler");
        assertTrue(dbOrder < businessOrder, "DatabaseExceptionHandler should run before BusinessExceptionHandler");
        assertTrue(businessOrder < runtimeOrder, "BusinessExceptionHandler should run before JavaRuntimeExceptionHandler");
        assertTrue(runtimeOrder < fallbackOrder, "JavaRuntimeExceptionHandler should run before FallbackExceptionHandler");
        assertEquals(Ordered.LOWEST_PRECEDENCE, fallbackOrder, "FallbackExceptionHandler must be lowest precedence");
    }
}

