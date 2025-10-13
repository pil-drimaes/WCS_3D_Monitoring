package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.AntRobotEtlEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "etl.antRobot", name = "enabled", havingValue = "true")
public class AntRobotScheduler extends BaseETLScheduler<AntRobotInfoRecord> {

    private final AntRobotEtlEngine engine;
    private final Set<String> processed = java.util.Collections.synchronizedSet(new HashSet<>());
    private boolean initialDone = false;

    @Autowired
    public AntRobotScheduler(AntRobotEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Scheduled(fixedRateString = "${etl.antRobot.interval}", initialDelayString = "${etl.antRobot.initialDelay}")
    public void executeETLProcess() {
        super.executeETLProcess();
    }

    @Override
    protected ETLEngine<AntRobotInfoRecord> getETLEngine() { return engine; }

    @Override
    protected String getSchedulerName() { return "ANT Robot"; }

    @Override
    protected void processInitialData() {
        if (initialDone) return;
        try {
            List<AntRobotInfoRecord> initial = engine.executeETL();
            for (AntRobotInfoRecord r : initial) processed.add(r.getUuid());
            initialDone = true;
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<AntRobotInfoRecord> rows = engine.executeETL();
            for (AntRobotInfoRecord r : rows) processed.add(r.getUuid());
            if (processed.size() > 10000) processed.clear();
        } catch (Exception ignore) {
        }
    }

    @Override
    public void clearSchedulerCache() {
        processed.clear();
        lastProcessedTime.set(null);
        initialDone = false;
    }
} 