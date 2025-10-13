package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.AntFlypickEtlEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "etl.antFlypick", name = "enabled", havingValue = "true")
public class AntFlypickScheduler extends BaseETLScheduler<AntFlypickInfoRecord> {

    private final AntFlypickEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public AntFlypickScheduler(AntFlypickEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Scheduled(fixedRateString = "${etl.antFlypick.interval}", initialDelayString = "${etl.antFlypick.initialDelay}")
    public void executeETLProcess() { super.executeETLProcess(); }

    @Override
    protected ETLEngine<AntFlypickInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "ANT FLYPICK"; }

    @Override
    protected void processInitialData() {
        if (initialDone) return;
        try {
            List<AntFlypickInfoRecord> initial = engine.executeETL();
            for (AntFlypickInfoRecord r : initial) processed.add(r.getUuid());
            initialDone = true;
        } catch (Exception ignore) { }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<AntFlypickInfoRecord> rows = engine.executeETL();
            for (AntFlypickInfoRecord r : rows) processed.add(r.getUuid());
            if (processed.size() > 10000) processed.clear();
        } catch (Exception ignore) { }
    }

    @Override
    public void clearSchedulerCache() {
        processed.clear();
        lastProcessedTime.set(null);
        initialDone = false;
    }
} 