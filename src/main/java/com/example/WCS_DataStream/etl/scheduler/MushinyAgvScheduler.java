package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.MushinyAgvEtlEngine;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyAgvInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class MushinyAgvScheduler extends BaseETLScheduler<MushinyAgvInfoRecord> {

    private final MushinyAgvEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public MushinyAgvScheduler(MushinyAgvEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    // 동적 스케줄링으로 대체

    @Override
    protected ETLEngine<MushinyAgvInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "MUSHINY AGV"; }

    @Override
    protected String getDomainKey() { return "mushinyagv"; }

    @Override
    protected void processInitialData() {
        if (initialDone) return;
        try {
            List<MushinyAgvInfoRecord> initial = engine.executeETL();
            for (MushinyAgvInfoRecord r : initial) processed.add(r.getUuid());
            initialDone = true;
        } catch (Exception ignore) { }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<MushinyAgvInfoRecord> rows = engine.executeETL();
            for (MushinyAgvInfoRecord r : rows) processed.add(r.getUuid());
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