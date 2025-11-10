package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.CapaDayViewEtlEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.view.CapaDayViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CapaDayViewScheduler extends BaseETLScheduler<CapaDayViewRow> {

    private final CapaDayViewEtlEngine engine;
    private boolean initialDone = false;

    @Autowired
    public CapaDayViewScheduler(CapaDayViewEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Override protected ETLEngine<CapaDayViewRow> getETLEngine() { return engine; }
    @Override protected String getSchedulerName() { return "CAPA DAY VIEW"; }
    @Override protected String getDomainKey() { return "capa_day_view"; }

    @Override protected void processInitialData() {
        if (initialDone) return;
        try { List<CapaDayViewRow> rows = engine.executeETL(); initialDone = true; } catch (Exception ignore) {}
    }

    @Override protected void processIncrementalData() {
        try { engine.executeETL(); } catch (Exception ignore) {}
    }

    @Override public void initializeOnStartup() {
        super.initializeOnStartup();
        var repo = SpringContext.getBean(com.example.WCS_DataStream.etl.service.SystemScheduleConfigRepository.class);
        if (repo != null) repo.ensureDomainRow(getDomainKey(), 5000L, 0L, "5s CAPA DAY VIEW sync");
    }

    @Override public void clearSchedulerCache() { lastProcessedTime.set(null); initialDone = false; }
}


