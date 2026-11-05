package in.blackshirt.common.logging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;

/**
 * Utility to read current tracing identifiers.
 * Extracts identifiers from Micrometer {@link Tracer} or falls back to SLF4J MDC.
 */
public final class TraceContext {

    private TraceContext() {}

    /**
     * Extracts traceId and spanId from the current Micrometer span,
     * or falls back to SLF4J MDC if no tracer/span is active.
     */
    public static TraceIds current(Tracer tracer) {
        if (tracer != null) {
            Span span = tracer.currentSpan();
            if (span != null && span.context() != null) {
                return new TraceIds(span.context().traceId(), span.context().spanId());
            }
        }
        String mdcTrace = MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY);
        String mdcSpan = MDC.get(RequestResponseLoggingFilter.SPAN_ID_MDC_KEY);
        if (mdcTrace != null || mdcSpan != null) {
            return new TraceIds(mdcTrace != null ? mdcTrace : "", mdcSpan != null ? mdcSpan : "");
        }
        return TraceIds.EMPTY;
    }

    /**
     * Convenience method to retrieve current traceId from MDC.
     */
    public static String getTraceId() {
        String mdcTrace = MDC.get(RequestResponseLoggingFilter.TRACE_ID_MDC_KEY);
        return mdcTrace != null ? mdcTrace : "";
    }

    /**
     * Convenience method to retrieve current spanId from MDC.
     */
    public static String getSpanId() {
        String mdcSpan = MDC.get(RequestResponseLoggingFilter.SPAN_ID_MDC_KEY);
        return mdcSpan != null ? mdcSpan : "";
    }

    public record TraceIds(String traceId, String spanId) {
        public static final TraceIds EMPTY = new TraceIds("", "");
    }
}
