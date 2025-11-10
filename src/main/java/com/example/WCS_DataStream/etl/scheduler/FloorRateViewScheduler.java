package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.FloorRateViewEtlEngine;
import com.example.WCS_DataStream.etl.model.view.FloorRateViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FloorRateViewScheduler extends BaseETLScheduler<FloorRateViewRow> {

    private final FloorRateViewEtlEngine engine;
    private boolean initialDone = false;

    @Autowired
    public FloorRateViewScheduler(FloorRateViewEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Override protected ETLEngine<FloorRateViewRow> getETLEngine() { return engine; }
    @Override protected String getSchedulerName() { return "FLOOR RATE VIEW"; }
    @Override protected String getDomainKey() { return "floor_rate_view"; }

    @Override protected void processInitialData() { if (initialDone) return; try { engine.executeETL(); initialDone = true; } catch (Exception ignore) {} }
    @Override protected void processIncrementalData() { try { engine.executeETL(); } catch (Exception ignore) {} }
    @Override public void clearSchedulerCache() { lastProcessedTime.set(null); initialDone = false; }

    @Override public void initializeOnStartup() {
        super.initializeOnStartup();
        var repo = SpringContext.getBean(com.example.WCS_DataStream.etl.service.SystemScheduleConfigRepository.class);
        if (repo != null) repo.ensureDomainRow(getDomainKey(), 5000L, 0L, "5s FLOOR RATE VIEW sync");
    }
}


