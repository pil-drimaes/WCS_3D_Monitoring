package com.example.WCS_DataStream.etl.common;

import com.example.WCS_DataStream.etl.engine.AgvDataETLEngine;
import com.example.WCS_DataStream.etl.engine.InventoryDataETLEngine;
import com.example.WCS_DataStream.etl.engine.PodDataETLEngine;
import com.example.WCS_DataStream.etl.ETLStatistics;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class EtlMetricsBinder {

    public EtlMetricsBinder(MeterRegistry registry,
                            AgvDataETLEngine agv,
                            InventoryDataETLEngine inv,
                            PodDataETLEngine pod) {
        // 최근 실행 시간 (통계의 lastExecutionTime 사용)
        Gauge.builder("etl_last_execution_epoch_ms", () -> toEpochMs(agv.getStatistics()))
                .tag("domain", "agv").register(registry);
        Gauge.builder("etl_last_execution_epoch_ms", () -> toEpochMs(inv.getStatistics()))
                .tag("domain", "inventory").register(registry);
        Gauge.builder("etl_last_execution_epoch_ms", () -> toEpochMs(pod.getStatistics()))
                .tag("domain", "pod").register(registry);

        // 누적 성공 건수 (통계의 successfulRecords 사용, 단조 증가)
        FunctionCounter.builder("etl_processed_records_total", agv,
                        e -> safeDouble(e.getStatistics().getSuccessfulRecords()))
                .tag("domain", "agv").register(registry);
        FunctionCounter.builder("etl_processed_records_total", inv,
                        e -> safeDouble(e.getStatistics().getSuccessfulRecords()))
                .tag("domain", "inventory").register(registry);
        FunctionCounter.builder("etl_processed_records_total", pod,
                        e -> safeDouble(e.getStatistics().getSuccessfulRecords()))
                .tag("domain", "pod").register(registry);
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