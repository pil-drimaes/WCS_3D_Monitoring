package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.CapaHourViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemCapaHourViewRepository;
import com.example.WCS_DataStream.etl.service.WcsCapaHourViewRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CapaHourViewEtlEngine extends ETLEngine<CapaHourViewRow> {

    private final WcsCapaHourViewRepository wcs;
    private final SystemCapaHourViewRepository systemRepo;

    public CapaHourViewEtlEngine(WcsCapaHourViewRepository wcs, SystemCapaHourViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override protected boolean checkTableExists() { return systemRepo.isConnected(); }
    @Override public boolean isConnected() { return systemRepo.isConnected(); }

    @Override protected List<CapaHourViewRow> extractData() throws ETLEngineException { return wcs.fetchAll(); }

    @Override
    protected List<CapaHourViewRow> transformAndLoad(List<CapaHourViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        LocalDate workDate = LocalDate.now();
        for (CapaHourViewRow r : data) {
            systemRepo.upsert(workDate, r.time(), r.qty(), r.type());
        }
        return data;
    }

    @Override protected String getDataKey(CapaHourViewRow data) { return data == null ? null : data.time(); }
    @Override protected boolean isSameData(CapaHourViewRow d1, CapaHourViewRow d2) { return false; }
    @Override public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


