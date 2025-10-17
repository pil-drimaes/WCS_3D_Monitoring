package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemAgvRepository;
import com.example.WCS_DataStream.etl.service.WcsAntRobotRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;
import com.example.WCS_DataStream.etl.service.KafkaEventPublisher;
import com.example.WCS_DataStream.etl.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class AntRobotEtlEngine extends ETLEngine<AntRobotInfoRecord> {

    private final WcsAntRobotRepository wcs;
    private final SystemAgvRepository systemRepo;
    private final EtlOffsetStore offsetStore;
    private final KafkaEventPublisher eventPublisher;
    private final RedisCacheService redis;
    private final String changeFields;

    private static final String JOB = "etl-ant-robot";
    private static final String SNAP_NS = "etlSnapshot:etl-ant-robot";

    public AntRobotEtlEngine(WcsAntRobotRepository wcs, SystemAgvRepository systemRepo, EtlOffsetStore offsetStore, KafkaEventPublisher eventPublisher, RedisCacheService redis, @Value("${etl.changeDetection.antRobot:}") String changeFields) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
        this.eventPublisher = eventPublisher;
        this.redis = redis;
        this.changeFields = changeFields;
    }

    @Override
    protected boolean checkTableExists() {
        return systemRepo.isConnected();
    }

    @Override
    public boolean isConnected() {
        return systemRepo.isConnected();
    }

    @Override
    protected List<AntRobotInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<AntRobotInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<AntRobotInfoRecord> transformAndLoad(List<AntRobotInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<AntRobotInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (AntRobotInfoRecord r : data) {
            AntRobotInfoRecord prev = redis.get(SNAP_NS, r.getUuid(), AntRobotInfoRecord.class);
            // 선택 필드 기준 변화 없으면 스킵
            if (!hasSelectedFieldsChanged(prev, r)) {
                continue;
            }

            systemRepo.upsertAntRobotInfo(r);
            eventPublisher.publishAntRobot(r);
            written.add(r);
            // 최신 스냅샷 저장
            redis.set(SNAP_NS, r.getUuid(), r);
            if (r.getUpdDt() != null) {
                if (maxTs == null || r.getUpdDt().after(maxTs) || (r.getUpdDt().equals(maxTs) && compareUuid(r.getUuid(), maxUuid) > 0)) {
                    maxTs = r.getUpdDt();
                    maxUuid = r.getUuid();
                }
            } else if (r.getInsDt() != null) {
                if (maxTs == null || r.getInsDt().after(maxTs) || (r.getInsDt().equals(maxTs) && compareUuid(r.getUuid(), maxUuid) > 0)) {
                    maxTs = r.getInsDt();
                    maxUuid = r.getUuid();
                }
            }
        }
        if (maxTs != null) {
            offsetStore.set(JOB, new EtlOffsetStore.Offset(maxTs, maxUuid));
        }
        return written;
    }

    private int compareUuid(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    @Override
    protected String getDataKey(AntRobotInfoRecord data) {
        return data.getUuid();
    }

    @Override
    protected boolean isSameData(AntRobotInfoRecord d1, AntRobotInfoRecord d2) {
        if (d1 == null || d2 == null) return false;
        return d1.getUuid() != null && d1.getUuid().equals(d2.getUuid());
    }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) {
        super.initialize(config, postgreSQLDataService);
    }

    private boolean hasSelectedFieldsChanged(AntRobotInfoRecord prev, AntRobotInfoRecord curr) {
        if (curr == null) return false;
        // prev가 없으면 신규로 간주하여 처리
        if (prev == null) return true;
        // 설정이 비어있으면 항상 처리(명시적으로 필드 지정 시에만 비교)
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
                case "nodeid":
                    if (!equalsObj(prev.getNodeId(), curr.getNodeId())) return true; else break;
                case "nexttarget":
                    if (!equalsObj(prev.getNextTarget(), curr.getNextTarget())) return true; else break;
                case "taskid":
                    if (!equalsObj(prev.getTaskId(), curr.getTaskId())) return true; else break;
                case "battery":
                    if (!equalsDecimal(prev.getBattery(), curr.getBattery())) return true; else break;
                case "status":
                    if (!equalsObj(prev.getStatus(), curr.getStatus())) return true; else break;
                case "manual":
                    if (!equalsObj(prev.getManual(), curr.getManual())) return true; else break;
                case "loaders":
                    if (!equalsObj(prev.getLoaders(), curr.getLoaders())) return true; else break;
                case "mapcode":
                    if (!equalsObj(prev.getMapCode(), curr.getMapCode())) return true; else break;
                case "robottype":
                    if (!equalsObj(prev.getRobotType(), curr.getRobotType())) return true; else break;
                case "robotno":
                    if (!equalsObj(prev.getRobotNo(), curr.getRobotNo())) return true; else break;
                case "podid":
                    if (!equalsObj(prev.getPodId(), curr.getPodId())) return true; else break;
                default:
                    // 알 수 없는 필드명은 무시
                    break;
            }
        }
        // 모든 지정 필드가 동일하면 변화 없음 → 처리 스킵
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