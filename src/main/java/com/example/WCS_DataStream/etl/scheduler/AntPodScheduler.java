package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.AntPodEtlEngine;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntPodInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "etl.antPod", name = "enabled", havingValue = "true")
public class AntPodScheduler extends BaseETLScheduler<AntPodInfoRecord> {

    private final AntPodEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public AntPodScheduler(AntPodEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Scheduled(fixedRateString = "${etl.antPod.interval}", initialDelayString = "${etl.antPod.initialDelay}")
    public void executeETLProcess() { super.executeETLProcess(); }

    @Override
    protected ETLEngine<AntPodInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "ANT POD"; }

    @Override
    protected void processInitialData() {
        if (initialDone) return;
        try {
            List<AntPodInfoRecord> initial = engine.executeETL();
            for (AntPodInfoRecord r : initial) processed.add(r.getUuid());
            initialDone = true;
        } catch (Exception ignore) { }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<AntPodInfoRecord> rows = engine.executeETL();
            for (AntPodInfoRecord r : rows) processed.add(r.getUuid());
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