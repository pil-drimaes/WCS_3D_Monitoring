package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.FloorRateViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemFloorRateViewRepository;
import com.example.WCS_DataStream.etl.service.WcsFloorRateViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FloorRateViewEtlEngine extends ETLEngine<FloorRateViewRow> {

    private final WcsFloorRateViewRepository wcs;
    private final SystemFloorRateViewRepository systemRepo;

    public FloorRateViewEtlEngine(WcsFloorRateViewRepository wcs, SystemFloorRateViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override protected boolean checkTableExists() { return systemRepo.isConnected(); }
    @Override public boolean isConnected() { return systemRepo.isConnected(); }

    @Override protected List<FloorRateViewRow> extractData() throws ETLEngineException { return wcs.fetchAll(); }

    @Override
    protected List<FloorRateViewRow> transformAndLoad(List<FloorRateViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        for (FloorRateViewRow r : data) {
            systemRepo.upsert(r.zoneCd(), r.floor(), r.rate(), r.qty());
        }
        return data;
    }

    @Override protected String getDataKey(FloorRateViewRow data) { return data == null ? null : data.zoneCd() + "|" + data.floor(); }
    @Override protected boolean isSameData(FloorRateViewRow d1, FloorRateViewRow d2) { return false; }
    @Override public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


