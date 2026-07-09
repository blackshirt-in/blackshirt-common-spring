package in.blackshirt.common.logging;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;

/**
 * Utility to read current tracing identifiers.
 * No dependency on the response model.
 */
public final class TraceContext {

    private TraceContext() {}

    /**
     * Extracts traceId and spanId from the current Micrometer span.
     * Returns empty strings if no tracer or no span is active.
     */
    public static TraceIds current(Tracer tracer) {
        if (tracer == null) {
            return TraceIds.EMPTY;
        }
        Span span = tracer.currentSpan();
        if (span == null) {
            return TraceIds.EMPTY;
        }
        return new TraceIds(span.context().traceId(), span.context().spanId());
    }

    public record TraceIds(String traceId, String spanId) {
        public static final TraceIds EMPTY = new TraceIds("", "");
    }
}
