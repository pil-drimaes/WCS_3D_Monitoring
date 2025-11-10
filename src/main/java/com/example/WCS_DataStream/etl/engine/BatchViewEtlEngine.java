package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.BatchViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemBatchViewRepository;
import com.example.WCS_DataStream.etl.service.WcsBatchViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchViewEtlEngine extends ETLEngine<BatchViewRow> {

    private final WcsBatchViewRepository wcs;
    private final SystemBatchViewRepository systemRepo;

    public BatchViewEtlEngine(WcsBatchViewRepository wcs, SystemBatchViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override protected boolean checkTableExists() { return systemRepo.isConnected(); }
    @Override public boolean isConnected() { return systemRepo.isConnected(); }
    @Override protected List<BatchViewRow> extractData() throws ETLEngineException { return wcs.fetchAll(); }

    @Override
    protected List<BatchViewRow> transformAndLoad(List<BatchViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        for (BatchViewRow r : data) {
            systemRepo.upsert(r.batchNo(), r.startTime(), r.endTime());
        }
        return data;
    }

    @Override protected String getDataKey(BatchViewRow data) { return data == null ? null : data.batchNo(); }
    @Override protected boolean isSameData(BatchViewRow d1, BatchViewRow d2) { return false; }
    @Override public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


