package com.bankmind.util;

import org.slf4j.MDC;
import java.util.UUID;

public class CorrelationIdUtil {

    private static final String MDC_KEY = "correlationId";

    private CorrelationIdUtil() {}

    public static String getOrGenerate(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return headerValue;
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
