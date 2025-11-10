package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.ZoneRateViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemZoneRateViewRepository;
import com.example.WCS_DataStream.etl.service.WcsZoneRateViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZoneRateViewEtlEngine extends ETLEngine<ZoneRateViewRow> {

    private final WcsZoneRateViewRepository wcs;
    private final SystemZoneRateViewRepository systemRepo;

    public ZoneRateViewEtlEngine(WcsZoneRateViewRepository wcs, SystemZoneRateViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override protected boolean checkTableExists() { return systemRepo.isConnected(); }
    @Override public boolean isConnected() { return systemRepo.isConnected(); }
    @Override protected List<ZoneRateViewRow> extractData() throws ETLEngineException { return wcs.fetchAll(); }

    @Override
    protected List<ZoneRateViewRow> transformAndLoad(List<ZoneRateViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        for (ZoneRateViewRow r : data) {
            systemRepo.upsert(r.zoneCd(), r.rate(), r.qty());
        }
        return data;
    }

    @Override protected String getDataKey(ZoneRateViewRow data) { return data == null ? null : data.zoneCd(); }
    @Override protected boolean isSameData(ZoneRateViewRow d1, ZoneRateViewRow d2) { return false; }
    @Override public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


