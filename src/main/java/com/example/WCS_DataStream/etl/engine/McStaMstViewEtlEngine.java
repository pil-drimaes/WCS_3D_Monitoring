package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.McStaMstViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemMcStaMstViewRepository;
import com.example.WCS_DataStream.etl.service.WcsMcStaMstViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McStaMstViewEtlEngine extends ETLEngine<McStaMstViewRow> {

    private final WcsMcStaMstViewRepository wcs;
    private final SystemMcStaMstViewRepository systemRepo;

    public McStaMstViewEtlEngine(WcsMcStaMstViewRepository wcs, SystemMcStaMstViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override protected boolean checkTableExists() { return systemRepo.isConnected(); }
    @Override public boolean isConnected() { return systemRepo.isConnected(); }
    @Override protected List<McStaMstViewRow> extractData() throws ETLEngineException { return wcs.fetchAll(); }

    @Override
    protected List<McStaMstViewRow> transformAndLoad(List<McStaMstViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        for (McStaMstViewRow r : data) {
            systemRepo.upsert(r.mcTyp(), r.mcNo(), r.mcAreaTyp(), r.mcNm(), r.errorCode());
        }
        return data;
    }

    @Override protected String getDataKey(McStaMstViewRow data) { return data == null ? null : data.mcTyp() + "|" + data.mcNo(); }
    @Override protected boolean isSameData(McStaMstViewRow d1, McStaMstViewRow d2) { return false; }
    @Override public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


