package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.MushinyPodEtlEngine;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyPodInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "etl.mushinyPod", name = "enabled", havingValue = "true")
public class MushinyPodScheduler extends BaseETLScheduler<MushinyPodInfoRecord> {

    private final MushinyPodEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public MushinyPodScheduler(MushinyPodEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Scheduled(fixedRateString = "${etl.mushinyPod.interval}", initialDelayString = "${etl.mushinyPod.initialDelay}")
    public void executeETLProcess() { super.executeETLProcess(); }

    @Override
    protected ETLEngine<MushinyPodInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "MUSHINY POD"; }

    @Override
    protected void processInitialData() {
        if (initialDone) return;
        try {
            List<MushinyPodInfoRecord> initial = engine.executeETL();
            for (MushinyPodInfoRecord r : initial) processed.add(r.getUuid());
            initialDone = true;
        } catch (Exception ignore) { }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<MushinyPodInfoRecord> rows = engine.executeETL();
            for (MushinyPodInfoRecord r : rows) processed.add(r.getUuid());
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