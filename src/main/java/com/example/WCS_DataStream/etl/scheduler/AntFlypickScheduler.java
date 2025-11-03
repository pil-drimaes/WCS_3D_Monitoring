package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.AntFlypickEtlEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AntFlypickScheduler extends BaseETLScheduler<AntFlypickInfoRecord> {

    private final AntFlypickEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public AntFlypickScheduler(AntFlypickEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    // 동적 스케줄링으로 대체

    @Override
    protected ETLEngine<AntFlypickInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "ANT FLYPICK"; }

    @Override
    protected String getDomainKey() { return "antflypick"; }

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