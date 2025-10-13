package com.example.WCS_DataStream.etl.common;


import com.example.WCS_DataStream.etl.engine.AntRobotEtlEngine;
import com.example.WCS_DataStream.etl.engine.MushinyAgvEtlEngine;
import com.example.WCS_DataStream.etl.engine.AntFlypickEtlEngine;
import com.example.WCS_DataStream.etl.engine.AntPodEtlEngine;
import com.example.WCS_DataStream.etl.engine.MushinyPodEtlEngine;
import com.example.WCS_DataStream.etl.ETLStatistics;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class EtlMetricsBinder {

    public EtlMetricsBinder(MeterRegistry registry,
                            ObjectProvider<AntRobotEtlEngine> antRobot,
                            ObjectProvider<MushinyAgvEtlEngine> mushinyAgv,
                            ObjectProvider<AntFlypickEtlEngine> antFlypick,
                            ObjectProvider<AntPodEtlEngine> antPod,
                            ObjectProvider<MushinyPodEtlEngine> mushinyPod) {

        antRobot.ifAvailable(e -> registerDomain(registry, "antRobot", e));
        mushinyAgv.ifAvailable(e -> registerDomain(registry, "mushinyAgv", e));
        antFlypick.ifAvailable(e -> registerDomain(registry, "antFlypick", e));
        antPod.ifAvailable(e -> registerDomain(registry, "antPod", e));
        mushinyPod.ifAvailable(e -> registerDomain(registry, "mushinyPod", e));
    }

    private static void registerDomain(MeterRegistry registry, String domain, Object engine) {
        // 최근 실행 시간
        Gauge.builder("etl_last_execution_epoch_ms", () -> toEpochMs(((com.example.WCS_DataStream.etl.engine.ETLEngine<?>) engine).getStatistics()))
                .tag("domain", domain).register(registry);
        // 누적 성공 건수
        FunctionCounter.builder("etl_processed_records_total",
                        (com.example.WCS_DataStream.etl.engine.ETLEngine<?>) engine,
                        e -> safeDouble(e.getStatistics().getSuccessfulRecords()))
                .tag("domain", domain).register(registry);
    }

    private static double toEpochMs(ETLStatistics stats) {
        LocalDateTime t = stats != null ? stats.getLastExecutionTime() : null;
        if (t == null) return 0d;
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

    private static double safeDouble(Long v) {
        return v == null ? 0d : v.doubleValue();
    }
} 