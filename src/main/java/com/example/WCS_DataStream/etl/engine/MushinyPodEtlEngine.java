package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyPodInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemMushinyPodRepository;
import com.example.WCS_DataStream.etl.service.WcsMushinyPodRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class MushinyPodEtlEngine extends ETLEngine<MushinyPodInfoRecord> {

    private final WcsMushinyPodRepository wcs;
    private final SystemMushinyPodRepository systemRepo;
    private final EtlOffsetStore offsetStore;

    private static final String JOB = "etl-mushiny-pod";

    public MushinyPodEtlEngine(WcsMushinyPodRepository wcs, SystemMushinyPodRepository systemRepo, EtlOffsetStore offsetStore) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
    }

    @Override
    protected boolean checkTableExists() { return systemRepo.isConnected(); }

    @Override
    public boolean isConnected() { return systemRepo.isConnected(); }

    @Override
    protected List<MushinyPodInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<MushinyPodInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<MushinyPodInfoRecord> transformAndLoad(List<MushinyPodInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<MushinyPodInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (MushinyPodInfoRecord r : data) {
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

    private int compareUuid(String a, String b) { if (a == null && b == null) return 0; if (a == null) return -1; if (b == null) return 1; return a.compareTo(b); }

    @Override
    protected String getDataKey(MushinyPodInfoRecord data) { return data.getUuid(); }

    @Override
    protected boolean isSameData(MushinyPodInfoRecord d1, MushinyPodInfoRecord d2) { return d1 != null && d2 != null && d1.getUuid() != null && d1.getUuid().equals(d2.getUuid()); }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
} 