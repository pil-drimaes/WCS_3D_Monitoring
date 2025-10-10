package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemAntFlypickRepository;
import com.example.WCS_DataStream.etl.service.WcsAntFlypickRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class AntFlypickEtlEngine extends ETLEngine<AntFlypickInfoRecord> {

    private final WcsAntFlypickRepository wcs;
    private final SystemAntFlypickRepository systemRepo;
    private final EtlOffsetStore offsetStore;

    private static final String JOB = "etl-ant-flypick";

    public AntFlypickEtlEngine(WcsAntFlypickRepository wcs, SystemAntFlypickRepository systemRepo, EtlOffsetStore offsetStore) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
    }

    @Override
    protected boolean checkTableExists() { return systemRepo.isConnected(); }

    @Override
    public boolean isConnected() { return systemRepo.isConnected(); }

    @Override
    protected List<AntFlypickInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<AntFlypickInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<AntFlypickInfoRecord> transformAndLoad(List<AntFlypickInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<AntFlypickInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (AntFlypickInfoRecord r : data) {
            systemRepo.upsert(r);
            written.add(r);
            Timestamp t = r.getUpdDt() != null ? r.getUpdDt() : r.getInsDt();
            if (t != null) {
                if (maxTs == null || t.after(maxTs) || (t.equals(maxTs) && compareUuid(r.getUuid(), maxUuid) > 0)) {
                    maxTs = t; maxUuid = r.getUuid();
                }
            }
        }
        if (maxTs != null) offsetStore.set(JOB, new EtlOffsetStore.Offset(maxTs, maxUuid));
        return written;
    }

    private int compareUuid(String a, String b) {
        if (a == null && b == null) return 0; if (a == null) return -1; if (b == null) return 1; return a.compareTo(b);
    }

    @Override
    protected String getDataKey(AntFlypickInfoRecord data) { return data.getUuid(); }

    @Override
    protected boolean isSameData(AntFlypickInfoRecord d1, AntFlypickInfoRecord d2) {
        return d1 != null && d2 != null && d1.getUuid() != null && d1.getUuid().equals(d2.getUuid());
    }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
} 