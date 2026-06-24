package com.ticketsale.common.util;

import java.util.UUID;

public final class CorrelationIdUtil {

    private CorrelationIdUtil() {
    }

    public static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }
}