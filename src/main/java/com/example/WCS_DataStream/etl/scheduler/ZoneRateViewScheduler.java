package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.ZoneRateViewEtlEngine;
import com.example.WCS_DataStream.etl.model.view.ZoneRateViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ZoneRateViewScheduler extends BaseETLScheduler<ZoneRateViewRow> {

    private final ZoneRateViewEtlEngine engine;
    private boolean initialDone = false;

    @Autowired
    public ZoneRateViewScheduler(ZoneRateViewEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Override protected ETLEngine<ZoneRateViewRow> getETLEngine() { return engine; }
    @Override protected String getSchedulerName() { return "ZONE RATE VIEW"; }
    @Override protected String getDomainKey() { return "zone_rate_view"; }

    @Override protected void processInitialData() { if (initialDone) return; try { engine.executeETL(); initialDone = true; } catch (Exception ignore) {} }
    @Override protected void processIncrementalData() { try { engine.executeETL(); } catch (Exception ignore) {} }
    @Override public void clearSchedulerCache() { lastProcessedTime.set(null); initialDone = false; }

    @Override public void initializeOnStartup() {
        super.initializeOnStartup();
        var repo = SpringContext.getBean(com.example.WCS_DataStream.etl.service.SystemScheduleConfigRepository.class);
        if (repo != null) repo.ensureDomainRow(getDomainKey(), 5000L, 0L, "5s ZONE RATE VIEW sync");
    }
}


