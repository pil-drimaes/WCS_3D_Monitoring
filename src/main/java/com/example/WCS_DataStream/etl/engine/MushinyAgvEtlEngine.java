package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyAgvInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemMushinyAgvRepository;
import com.example.WCS_DataStream.etl.service.WcsMushinyAgvRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;
import com.example.WCS_DataStream.etl.service.KafkaEventPublisher;
import com.example.WCS_DataStream.etl.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class MushinyAgvEtlEngine extends ETLEngine<MushinyAgvInfoRecord> {

    private final WcsMushinyAgvRepository wcs;
    private final SystemMushinyAgvRepository systemRepo;
    private final EtlOffsetStore offsetStore;
    private final KafkaEventPublisher eventPublisher;
    private final RedisCacheService redis;
    private final String changeFields;

    private static final String JOB = "etl-mushiny-agv";
    private static final String SNAP_NS = "etlSnapshot:etl-mushiny-agv";

    public MushinyAgvEtlEngine(WcsMushinyAgvRepository wcs, SystemMushinyAgvRepository systemRepo, EtlOffsetStore offsetStore, KafkaEventPublisher eventPublisher, RedisCacheService redis, @Value("${etl.changeDetection.mushinyAgv:}") String changeFields) {
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
    protected List<MushinyAgvInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<MushinyAgvInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<MushinyAgvInfoRecord> transformAndLoad(List<MushinyAgvInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<MushinyAgvInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (MushinyAgvInfoRecord r : data) {
            MushinyAgvInfoRecord prev = redis.get(SNAP_NS, r.getUuid(), MushinyAgvInfoRecord.class);
            if (!hasSelectedFieldsChanged(prev, r)) {
                continue;
            }
            systemRepo.upsert(r);
            eventPublisher.publishMushinyAgv(r);
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
    protected String getDataKey(MushinyAgvInfoRecord data) { return data.getUuid(); }

    @Override
    protected boolean isSameData(MushinyAgvInfoRecord d1, MushinyAgvInfoRecord d2) {
        return d1 != null && d2 != null && d1.getUuid() != null && d1.getUuid().equals(d2.getUuid());
    }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }

    private boolean hasSelectedFieldsChanged(MushinyAgvInfoRecord prev, MushinyAgvInfoRecord curr) {
        if (curr == null) return false;
        if (prev == null) return true;
        String cfg = changeFields == null ? "" : changeFields.trim();
        if (cfg.isEmpty()) return true;
        String[] fields = cfg.split(",");
        for (String f : fields) {
            String key = f.trim().toLowerCase();
            if (key.isEmpty()) continue;
            switch (key) {
                case "posx": if (!equalsDecimal(prev.getPosX(), curr.getPosX())) return true; else break;
                case "posy": if (!equalsDecimal(prev.getPosY(), curr.getPosY())) return true; else break;
                case "zonecode": if (!equalsObj(prev.getZoneCode(), curr.getZoneCode())) return true; else break;
                case "speed": if (!equalsDecimal(prev.getBattery(), curr.getBattery())) return true; else break; // or speed if exists
                case "nodeid": if (!equalsObj(prev.getNodeId(), curr.getNodeId())) return true; else break;
                case "poddirection": if (!equalsObj(prev.getPodDirection(), curr.getPodDirection())) return true; else break;
                case "haspod": if (!equalsObj(prev.getHasPod(), curr.getHasPod())) return true; else break;
                default: break;
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