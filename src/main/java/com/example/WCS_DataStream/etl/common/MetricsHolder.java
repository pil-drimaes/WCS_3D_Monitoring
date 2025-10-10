package com.example.WCS_DataStream.etl.common;

import io.micrometer.core.instrument.Counter;

public final class MetricsHolder {
    private static volatile Counter agvProcessed;
    private static volatile Counter invProcessed;
    private static volatile Counter podProcessed;

    private MetricsHolder() {}

    public static void setAgvProcessed(Counter c) { agvProcessed = c; }
    public static void setInvProcessed(Counter c) { invProcessed = c; }
    public static void setPodProcessed(Counter c) { podProcessed = c; }

    public static Counter getAgvProcessed() { return agvProcessed; }
    public static Counter getInvProcessed() { return invProcessed; }
    public static Counter getPodProcessed() { return podProcessed; }
} 