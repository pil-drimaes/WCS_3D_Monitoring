package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntPodInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemAntPodRepository;
import com.example.WCS_DataStream.etl.service.WcsAntPodRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;
import com.example.WCS_DataStream.etl.service.KafkaEventPublisher;
import com.example.WCS_DataStream.etl.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class AntPodEtlEngine extends ETLEngine<AntPodInfoRecord> {

    private final WcsAntPodRepository wcs;
    private final SystemAntPodRepository systemRepo;
    private final EtlOffsetStore offsetStore;
    private final KafkaEventPublisher eventPublisher;
    private final RedisCacheService redis;
    private final String changeFields;

    private static final String JOB = "etl-ant-pod";
    private static final String SNAP_NS = "etlSnapshot:etl-ant-pod";

    public AntPodEtlEngine(WcsAntPodRepository wcs, SystemAntPodRepository systemRepo, EtlOffsetStore offsetStore, KafkaEventPublisher eventPublisher, RedisCacheService redis, @Value("${etl.changeDetection.antPod:}") String changeFields) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
        this.eventPublisher = eventPublisher;
        this.redis = redis;
        this.changeFields = changeFields;
    }

    @Override
    protected boolean checkTableExists() { return systemRepo.isConnected(); }

    @Override
    public boolean isConnected() { return systemRepo.isConnected(); }

    @Override
    protected List<AntPodInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<AntPodInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<AntPodInfoRecord> transformAndLoad(List<AntPodInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<AntPodInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (AntPodInfoRecord r : data) {
            AntPodInfoRecord prev = redis.get(SNAP_NS, r.getUuid(), AntPodInfoRecord.class);
            if (!hasSelectedFieldsChanged(prev, r)) {
                continue;
            }
            systemRepo.upsert(r);
            eventPublisher.publishAntPod(r);
            written.add(r);
            redis.set(SNAP_NS, r.getUuid(), r);
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
    protected String getDataKey(AntPodInfoRecord data) { return data.getUuid(); }

    @Override
    protected boolean isSameData(AntPodInfoRecord d1, AntPodInfoRecord d2) { return d1 != null && d2 != null && d1.getUuid() != null && d1.getUuid().equals(d2.getUuid()); }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }

    private boolean hasSelectedFieldsChanged(AntPodInfoRecord prev, AntPodInfoRecord curr) {
        if (curr == null) return false;
        if (prev == null) return true;
        String cfg = changeFields == null ? "" : changeFields.trim();
        if (cfg.isEmpty()) return true;
        String[] fields = cfg.split(",");
        for (String f : fields) {
            String key = f.trim().toLowerCase();
            if (key.isEmpty()) continue;
            switch (key) {
                case "location":
                    if (!equalsObj(prev.getLocation(), curr.getLocation())) return true; else break;
                case "podface":
                    if (!equalsObj(prev.getPodFace(), curr.getPodFace())) return true; else break;
                case "podid":
                    if (!equalsObj(prev.getPodId(), curr.getPodId())) return true; else break;
                default:
                    break;
            }
        }
        return false;
    }

    private boolean equalsObj(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
} 