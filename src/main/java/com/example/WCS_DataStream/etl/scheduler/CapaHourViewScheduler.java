package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.CapaHourViewEtlEngine;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.model.view.CapaHourViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CapaHourViewScheduler extends BaseETLScheduler<CapaHourViewRow> {

    private final CapaHourViewEtlEngine engine;
    private boolean initialDone = false;

    @Autowired
    public CapaHourViewScheduler(CapaHourViewEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Override protected ETLEngine<CapaHourViewRow> getETLEngine() { return engine; }
    @Override protected String getSchedulerName() { return "CAPA HOUR VIEW"; }
    @Override protected String getDomainKey() { return "capa_hour_view"; }

    @Override protected void processInitialData() { if (initialDone) return; try { List<CapaHourViewRow> r = engine.executeETL(); initialDone = true; } catch (Exception ignore) {} }
    @Override protected void processIncrementalData() { try { engine.executeETL(); } catch (Exception ignore) {} }
    @Override public void clearSchedulerCache() { lastProcessedTime.set(null); initialDone = false; }

    @Override public void initializeOnStartup() {
        super.initializeOnStartup();
        var repo = SpringContext.getBean(com.example.WCS_DataStream.etl.service.SystemScheduleConfigRepository.class);
        if (repo != null) repo.ensureDomainRow(getDomainKey(), 5000L, 0L, "5s CAPA HOUR VIEW sync");
    }
}


