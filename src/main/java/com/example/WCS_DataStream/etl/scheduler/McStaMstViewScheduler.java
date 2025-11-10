package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.McStaMstViewEtlEngine;
import com.example.WCS_DataStream.etl.model.view.McStaMstViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McStaMstViewScheduler extends BaseETLScheduler<McStaMstViewRow> {

    private final McStaMstViewEtlEngine engine;
    private boolean initialDone = false;

    @Autowired
    public McStaMstViewScheduler(McStaMstViewEtlEngine engine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);
        this.engine = engine;
    }

    @Override protected ETLEngine<McStaMstViewRow> getETLEngine() { return engine; }
    @Override protected String getSchedulerName() { return "MC STATE MST VIEW"; }
    @Override protected String getDomainKey() { return "mc_sta_mst_view"; }

    @Override protected void processInitialData() { if (initialDone) return; try { engine.executeETL(); initialDone = true; } catch (Exception ignore) {} }
    @Override protected void processIncrementalData() { try { engine.executeETL(); } catch (Exception ignore) {} }
    @Override public void clearSchedulerCache() { lastProcessedTime.set(null); initialDone = false; }

    @Override public void initializeOnStartup() {
        super.initializeOnStartup();
        var repo = SpringContext.getBean(com.example.WCS_DataStream.etl.service.SystemScheduleConfigRepository.class);
        if (repo != null) repo.ensureDomainRow(getDomainKey(), 5000L, 0L, "5s MC STATE MST VIEW sync");
    }
}


