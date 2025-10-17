package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemAntFlypickRepository;
import com.example.WCS_DataStream.etl.service.WcsAntFlypickRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class AntFlypickEtlEngine extends ETLEngine<AntFlypickInfoRecord> {

    private final WcsAntFlypickRepository wcs;
    private final SystemAntFlypickRepository systemRepo;
    private final EtlOffsetStore offsetStore;
    private final RedisCacheService redis;
    private final String changeFields;

    private static final String JOB = "etl-ant-flypick";
    private static final String SNAP_NS = "etlSnapshot:etl-ant-flypick";

    public AntFlypickEtlEngine(WcsAntFlypickRepository wcs, SystemAntFlypickRepository systemRepo, EtlOffsetStore offsetStore, RedisCacheService redis, @Value("${etl.changeDetection.antFlypick:}") String changeFields) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
        this.redis = redis;
        this.changeFields = changeFields;
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
            AntFlypickInfoRecord prev = redis.get(SNAP_NS, r.getUuid(), AntFlypickInfoRecord.class);
            if (!hasSelectedFieldsChanged(prev, r)) {
                continue;
            }
            systemRepo.upsert(r);
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

    private boolean hasSelectedFieldsChanged(AntFlypickInfoRecord prev, AntFlypickInfoRecord curr) {
        if (curr == null) return false;
        if (prev == null) return true;
        String cfg = changeFields == null ? "" : changeFields.trim();
        if (cfg.isEmpty()) return true;
        String[] fields = cfg.split(",");
        for (String f : fields) {
            String key = f.trim().toLowerCase();
            if (key.isEmpty()) continue;
            switch (key) {
                case "posx":
                    if (!equalsDecimal(prev.getPosX(), curr.getPosX())) return true; else break;
                case "posy":
                    if (!equalsDecimal(prev.getPosY(), curr.getPosY())) return true; else break;
                case "zonecode":
                    if (!equalsObj(prev.getZoneCode(), curr.getZoneCode())) return true; else break;
                case "speed":
                    if (!equalsDecimal(prev.getSpeed(), curr.getSpeed())) return true; else break;
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

    private boolean equalsDecimal(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }
} 